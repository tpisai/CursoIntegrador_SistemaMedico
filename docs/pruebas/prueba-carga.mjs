// Prueba de rendimiento y concurrencia (EDT 5.2, RNF-01 y RNF-07).
//
// Ejecútala SOLO contra una base de prueba recién instalada: crea citas y una campaña.
//   powershell -ExecutionPolicy Bypass -File sql\instalar_bd.ps1 -Reiniciar -BaseDeDatos centro_salud_prueba
//   .\mvnw spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081 --spring.datasource.url=jdbc:postgresql://localhost:5432/centro_salud_prueba --spring.jpa.show-sql=false"
//   node docs/pruebas/prueba-carga.mjs http://localhost:8081
//
// No necesita instalar nada: usa solo Node.js 20 o superior.

const BASE = (process.argv[2] ?? "http://localhost:8081").replace(/\/$/, "") + "/api"
const USUARIOS = Number(process.argv[3] ?? 500)
const PETICIONES_POR_USUARIO = 10
const LIMITE_MS = 3000 // RNF-01

const PACIENTES = ["12345678", "45879621", "40125879", "42365981", "47851236"]

async function pedir(ruta, { metodo = "GET", token, cuerpo } = {}) {
  const inicio = performance.now()
  try {
    const respuesta = await fetch(BASE + ruta, {
      method: metodo,
      headers: {
        ...(cuerpo ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: cuerpo ? JSON.stringify(cuerpo) : undefined,
    })
    const texto = await respuesta.text()
    return { estado: respuesta.status, ms: performance.now() - inicio, datos: texto ? JSON.parse(texto) : null }
  } catch (error) {
    return { estado: 0, ms: performance.now() - inicio, datos: null, error: String(error) }
  }
}

const percentil = (ordenados, p) => ordenados[Math.min(ordenados.length - 1, Math.ceil((p / 100) * ordenados.length) - 1)]

function resumen(titulo, resultados) {
  const ms = resultados.map((r) => r.ms).sort((a, b) => a - b)
  const fallos = resultados.filter((r) => r.estado < 200 || r.estado >= 300).length
  const ok = percentil(ms, 95) <= LIMITE_MS && fallos === 0
  console.log(`\n${titulo}`)
  console.log(`  peticiones: ${resultados.length} · fallidas: ${fallos}`)
  console.log(`  mediana ${Math.round(percentil(ms, 50))} ms · p95 ${Math.round(percentil(ms, 95))} ms · p99 ${Math.round(percentil(ms, 99))} ms · máximo ${Math.round(ms.at(-1))} ms`)
  console.log(`  ${ok ? "CUMPLE" : "NO CUMPLE"}: p95 de hasta ${LIMITE_MS} ms y sin fallos`)
  return ok
}

async function entrar(dni, pin) {
  const { estado, datos } = await pedir("/auth/login", { metodo: "POST", cuerpo: { dni, pin } })
  if (estado !== 200) throw new Error(`No se pudo entrar con ${dni}: ${estado}`)
  return datos.token
}

function mesActual() {
  return new Date().toISOString().slice(0, 7)
}

// Busca un doctor con turnos libres para las pruebas.
async function turnosLibres() {
  const { datos: especialidades } = await pedir("/especialidades")
  const { datos: doctores } = await pedir(`/doctores?especialidad=${especialidades[0].idEspecialidad}`)
  const doctor = doctores[0]
  const { datos: consultorios } = await pedir(`/doctores/${doctor.idDoctor}/consultorios`)
  const consultorio = consultorios[0]
  const { datos: dias } = await pedir(`/horarios/disponibilidad?doctor=${doctor.idDoctor}&consultorio=${consultorio.idConsultorio}&mes=${mesActual()}`)
  const dia = dias.at(-1)
  const { datos: horarios } = await pedir(`/horarios?doctor=${doctor.idDoctor}&consultorio=${consultorio.idConsultorio}&fecha=${dia.fecha}`)
  return { doctor, consultorio, libres: horarios.filter((h) => h.estado === "DISPONIBLE") }
}

async function usuariosConcurrentes(tarea, usuarios, repeticiones) {
  const resultados = []
  await Promise.all(
    Array.from({ length: usuarios }, async () => {
      for (let i = 0; i < repeticiones; i++) resultados.push(await tarea())
    })
  )
  return resultados
}

console.log(`Servidor: ${BASE}`)
console.log(`Fecha: ${new Date().toISOString()}`)

const { doctor, consultorio, libres } = await turnosLibres()
const rutaDisponibilidad = `/horarios/disponibilidad?doctor=${doctor.idDoctor}&consultorio=${consultorio.idConsultorio}&mes=${mesActual()}`
let todoBien = true

// 1. RNF-01 y RNF-07: consulta de disponibilidad con usuarios simultáneos.
await pedir(rutaDisponibilidad) // calienta el servidor
for (const usuarios of [50, 200, USUARIOS]) {
  const resultados = await usuariosConcurrentes(() => pedir(rutaDisponibilidad), usuarios, PETICIONES_POR_USUARIO)
  todoBien = resumen(`1. Disponibilidad con ${usuarios} usuarios simultáneos`, resultados) && todoBien
}

// 2. Inicio de sesión simultáneo (BCrypt es costoso a propósito).
{
  const resultados = await usuariosConcurrentes(() => pedir("/auth/login", { metodo: "POST", cuerpo: { dni: "12345678", pin: "123456" } }), 50, 1)
  todoBien = resumen("2. Inicio de sesión con 50 usuarios simultáneos", resultados) && todoBien
}

// 3. Dos pacientes no pueden quedarse con el mismo turno.
{
  const tokens = await Promise.all(PACIENTES.map((dni) => entrar(dni, "123456")))
  const horario = libres[0]
  const intentos = await Promise.all(
    Array.from({ length: 100 }, (_, i) => pedir("/citas", { metodo: "POST", token: tokens[i % tokens.length], cuerpo: { idHorario: horario.idHorario } }))
  )
  const reservas = intentos.filter((r) => r.estado === 201).length
  const rechazos = intentos.filter((r) => r.estado === 409).length
  const ok = reservas === 1 && rechazos === 99
  console.log(`\n3. 100 intentos simultáneos por el mismo turno\n  reservas: ${reservas} · rechazadas con 409: ${rechazos} · otras: ${100 - reservas - rechazos}`)
  console.log(`  ${ok ? "CUMPLE" : "NO CUMPLE"}: exactamente 1 reserva`)
  todoBien = ok && todoBien
}

// 4. Una campaña no puede pasar de sus cupos.
{
  const admin = await entrar("44556677", "445566")
  const hoy = new Date().toISOString().slice(0, 10)
  const { datos: campania } = await pedir("/admin/campanias", {
    metodo: "POST",
    token: admin,
    cuerpo: { titulo: "Prueba de cupos", fechaInicio: hoy, fechaFin: hoy, cupos: 3 },
  })
  const tokens = await Promise.all(PACIENTES.map((dni) => entrar(dni, "123456")))
  const intentos = await Promise.all(tokens.map((token) => pedir(`/campanias/${campania.idCampania}/inscripcion`, { metodo: "POST", token })))
  const inscritos = intentos.filter((r) => r.estado >= 200 && r.estado < 300).length
  const ok = inscritos === 3
  console.log(`\n4. 5 pacientes simultáneos por 3 cupos\n  inscritos: ${inscritos} · rechazados: ${intentos.length - inscritos}`)
  console.log(`  ${ok ? "CUMPLE" : "NO CUMPLE"}: exactamente 3 inscripciones`)
  todoBien = ok && todoBien
}

console.log(`\nResultado general: ${todoBien ? "CUMPLE" : "NO CUMPLE"}`)
process.exitCode = todoBien ? 0 : 1
