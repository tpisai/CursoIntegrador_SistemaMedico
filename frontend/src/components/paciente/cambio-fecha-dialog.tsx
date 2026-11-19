"use client"

import { useState, useTransition } from "react"
import { useSWRConfig } from "swr"
import { toast } from "sonner"
import { CalendarClockIcon, CircleAlertIcon, StethoscopeIcon, XCircleIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Spinner } from "@/components/ui/spinner"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { CampoSelect } from "@/components/portal/campo-select"
import { useConsultorios, useDoctores, useHorarios, useMisCitas } from "@/hooks/use-citas"
import { useCambiosPendientes } from "@/hooks/use-solicitudes"
import { ApiError, mensajeDeError } from "@/lib/api/http"
import { RUTA_NOTIFICACIONES } from "@/lib/api/notificaciones"
import { resolverCambio, rutasSolicitudes } from "@/lib/api/solicitudes"
import type { CambioPendiente, DecisionCambio, ResolucionCambio } from "@/lib/api/types"
import { formatDiaHora, toFechaISO } from "@/lib/format"

// RF-17: administración aprobó el cambio de fecha; el paciente debe decidir antes de seguir.
export function CambioFechaPendienteDialog() {
  const { data, mutate } = useCambiosPendientes()
  const [resueltos, setResueltos] = useState<number[]>([])
  const actual = data?.find((cambio) => !resueltos.includes(cambio.idSolicitud))

  if (!actual) return null

  return (
    <ResolverCambioDialog
      key={actual.idSolicitud}
      cambio={actual}
      onCerrar={() => {
        setResueltos((ids) => [...ids, actual.idSolicitud])
        void mutate()
      }}
    />
  )
}

const OPCIONES: { decision: DecisionCambio; titulo: string; detalle: string; icono: typeof CalendarClockIcon }[] = [
  {
    decision: "MISMO_DOCTOR",
    titulo: "Cambiar solo la hora",
    detalle: "Elige otro turno con el mismo doctor.",
    icono: CalendarClockIcon,
  },
  {
    decision: "OTRO_DOCTOR",
    titulo: "Ver a otro doctor",
    detalle: "Reserva una cita con otro doctor de la misma especialidad y se cancela la actual.",
    icono: StethoscopeIcon,
  },
  {
    decision: "CANCELADA",
    titulo: "Cancelar mi cita",
    detalle: "El turno se libera para otro paciente.",
    icono: XCircleIcon,
  },
]

function ResolverCambioDialog({ cambio, onCerrar }: { cambio: CambioPendiente; onCerrar: () => void }) {
  const { mutate } = useSWRConfig()
  const misCitas = useMisCitas()
  const [decision, setDecision] = useState<DecisionCambio | null>(null)
  const [idDoctorElegido, setIdDoctorElegido] = useState("")
  const [idConsultorioElegido, setIdConsultorioElegido] = useState("")
  const [fecha, setFecha] = useState("")
  const [idHorario, setIdHorario] = useState<number | null>(null)
  const [resultado, setResultado] = useState<ResolucionCambio | null>(null)
  const [enviando, startTransition] = useTransition()

  const otroDoctor = decision === "OTRO_DOCTOR"
  const doctores = useDoctores(otroDoctor ? cambio.idEspecialidad : undefined)
  const opcionesDoctor = doctores.data?.filter((d) => d.idDoctor !== cambio.idDoctor)
  const idDoctor = otroDoctor ? Number(idDoctorElegido) || null : cambio.idDoctor

  const consultorios = useConsultorios(idDoctor ?? undefined)
  const consultorioOriginal = consultorios.data?.find((c) => c.idConsultorio === cambio.idConsultorio)?.idConsultorio
  const idConsultorio = idConsultorioElegido
    ? Number(idConsultorioElegido)
    : otroDoctor
      ? (consultorios.data?.[0]?.idConsultorio ?? null)
      : (consultorioOriginal ?? consultorios.data?.[0]?.idConsultorio ?? null)

  const horarios = useHorarios(idDoctor ?? undefined, idConsultorio ?? undefined, fecha || null)
  const libres = horarios.data?.filter((h) => h.estado === "DISPONIBLE") ?? []
  const horarioElegido = libres.find((h) => h.idHorario === idHorario)

  const hoy = new Date()
  const maximo = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() + 60)
  const pideTurno = decision === "MISMO_DOCTOR" || decision === "OTRO_DOCTOR"
  const puedeConfirmar = decision === "CANCELADA" || (pideTurno && horarioElegido !== undefined)

  function elegirDoctor(valor: string) {
    setIdDoctorElegido(valor)
    setIdConsultorioElegido("")
    setFecha("")
    setIdHorario(null)
  }

  function elegirConsultorio(valor: string) {
    setIdConsultorioElegido(valor)
    setFecha("")
    setIdHorario(null)
  }

  function elegirDecision(valor: DecisionCambio) {
    setDecision(valor)
    setIdHorario(null)
  }

  function confirmar() {
    if (!decision) return
    startTransition(async () => {
      try {
        const resolucion = await resolverCambio(cambio.idSolicitud, {
          decision,
          ...(pideTurno && idHorario !== null && { idHorarioNuevo: idHorario }),
        })
        setResultado(resolucion)
        await Promise.all([mutate(rutasSolicitudes.pendientes), misCitas.mutate(), mutate(RUTA_NOTIFICACIONES)])
      } catch (error) {
        toast.error(mensajeDeError(error))
        // Otro paciente ganó el turno: se refrescan los horarios para elegir otro.
        if (error instanceof ApiError && error.status === 409) {
          setIdHorario(null)
          await horarios.mutate()
        }
      }
    })
  }

  return (
    <Dialog open modal>
      <DialogContent
        showCloseButton={false}
        className="max-h-[90vh] overflow-y-auto sm:max-w-2xl"
        onEscapeKeyDown={(event) => event.preventDefault()}
        onInteractOutside={(event) => event.preventDefault()}
      >
        {resultado ? (
          <>
            <DialogHeader>
              <DialogTitle className="font-heading text-lg">Cambio de fecha resuelto</DialogTitle>
              <DialogDescription>{resultado.mensaje}</DialogDescription>
            </DialogHeader>
            <Button size="xl" className="w-full" onClick={onCerrar}>
              Entendido
            </Button>
          </>
        ) : (
          <>
            <DialogHeader>
              <DialogTitle className="font-heading text-lg">Tu cita necesita un nuevo horario</DialogTitle>
              <DialogDescription>
                Administración aprobó el cambio de fecha. Elige qué hacer para seguir usando tu portal.
              </DialogDescription>
            </DialogHeader>

            <div className="rounded-lg border bg-muted/40 p-3 text-sm">
              <p className="font-semibold">
                {cambio.doctor} · {cambio.especialidad}
              </p>
              <p className="text-muted-foreground">
                {formatDiaHora(cambio.fecha, cambio.hora)} · {cambio.consultorio}
              </p>
              <p className="mt-1 text-muted-foreground">Motivo del doctor: {cambio.motivo}</p>
              {cambio.respuestaAdmin ? (
                <p className="mt-1 text-muted-foreground">Administración: {cambio.respuestaAdmin}</p>
              ) : null}
            </div>

            <div className="grid gap-2 sm:grid-cols-3" role="group" aria-label="¿Qué quieres hacer con tu cita?">
              {OPCIONES.map(({ decision: valor, titulo, detalle, icono: Icono }) => {
                const activa = decision === valor
                return (
                  <button
                    key={valor}
                    type="button"
                    aria-pressed={activa}
                    onClick={() => elegirDecision(valor)}
                    className={`flex flex-col gap-1 rounded-lg border p-3 text-left text-sm transition-colors outline-none focus-visible:ring-2 focus-visible:ring-ring ${
                      activa ? "border-primary bg-primary/5 ring-1 ring-primary" : "hover:bg-muted/60"
                    }`}
                  >
                    <Icono className="size-4 text-primary" aria-hidden="true" />
                    <span className="font-semibold">{titulo}</span>
                    <span className="text-xs text-muted-foreground">{detalle}</span>
                  </button>
                )
              })}
            </div>

            {pideTurno ? (
              <div className="flex flex-col gap-3 rounded-lg border p-3">
                {otroDoctor ? (
                  <CampoSelect
                    id="cambio-doctor"
                    etiqueta="Doctor"
                    placeholder="Elige un doctor"
                    vacio="No hay otros doctores en esta especialidad"
                    opciones={opcionesDoctor?.map((d) => ({
                      valor: String(d.idDoctor),
                      texto: d.nombreCompleto,
                    }))}
                    valor={idDoctorElegido}
                    onCambio={elegirDoctor}
                  />
                ) : null}

                <CampoSelect
                  id="cambio-consultorio"
                  etiqueta="Consultorio"
                  placeholder="Elige un consultorio"
                  vacio="Este doctor no tiene consultorios"
                  opciones={consultorios.data?.map((c) => ({
                    valor: String(c.idConsultorio),
                    texto: c.nombre,
                  }))}
                  valor={idConsultorio !== null ? String(idConsultorio) : ""}
                  onCambio={elegirConsultorio}
                />

                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="cambio-fecha">Fecha</Label>
                  <Input
                    id="cambio-fecha"
                    type="date"
                    min={toFechaISO(hoy)}
                    max={toFechaISO(maximo)}
                    value={fecha}
                    disabled={idDoctor === null || idConsultorio === null}
                    onChange={(event) => {
                      setFecha(event.target.value)
                      setIdHorario(null)
                    }}
                  />
                </div>

                {fecha ? (
                  horarios.isLoading ? (
                    <p className="text-sm text-muted-foreground">Buscando horarios…</p>
                  ) : libres.length ? (
                    <ToggleGroup
                      type="single"
                      variant="slot"
                      size="lg"
                      spacing={2}
                      aria-label="Horarios disponibles"
                      className="grid w-full grid-cols-3 sm:grid-cols-4"
                      value={idHorario !== null ? String(idHorario) : ""}
                      onValueChange={(valor) => setIdHorario(valor ? Number(valor) : null)}
                    >
                      {libres.map((h) => (
                        <ToggleGroupItem key={h.idHorario} value={String(h.idHorario)} className="w-full">
                          {h.horaInicio}
                        </ToggleGroupItem>
                      ))}
                    </ToggleGroup>
                  ) : (
                    <Alert>
                      <CircleAlertIcon />
                      <AlertDescription>No hay turnos libres ese día. Prueba otra fecha.</AlertDescription>
                    </Alert>
                  )
                ) : null}
              </div>
            ) : null}

            <Button size="xl" className="w-full" disabled={!puedeConfirmar || enviando} onClick={confirmar}>
              {enviando ? <Spinner data-icon="inline-start" /> : null}
              {decision === "CANCELADA"
                ? "Sí, cancelar mi cita"
                : decision === "OTRO_DOCTOR"
                  ? "Reservar con el otro doctor"
                  : "Confirmar nuevo horario"}
            </Button>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}
