"use client"

import Link from "next/link"
import { ArrowLeftIcon, CalendarClockIcon, CircleAlertIcon, CircleCheckIcon, UserXIcon } from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { EstadoCitaBadge } from "@/components/citas/estado-cita-badge"
import { DetalleAtencion } from "@/components/consulta/detalle-atencion"
import { FichaConsulta } from "@/components/consulta/ficha-consulta"
import { HistoriaClinica } from "@/components/consulta/historia-clinica"
import { ReporteExamenes } from "@/components/consulta/reporte-examenes"
import { useConsulta } from "@/hooks/use-consulta"
import { mensajeDeError } from "@/lib/api/http"
import type { ConsultaDoctor as Consulta } from "@/lib/api/types"
import { formatDiaHora, formatFechaLarga } from "@/lib/format"

const SEXOS: Record<string, string> = { FEMENINO: "Femenino", MASCULINO: "Masculino" }

/** Una alergia registrada se destaca; "Ninguna conocida" no necesita alerta. */
function tieneAlergias(alergias: string | null): alergias is string {
  return !!alergias && !/^(ningun|no\b|sin\b)/i.test(alergias.trim())
}

function VolverAgenda() {
  return (
    <Button asChild variant="ghost" className="h-11 self-start px-2 text-muted-foreground sm:h-8">
      <Link href="/doctor/agenda">
        <ArrowLeftIcon data-icon="inline-start" />
        Agenda
      </Link>
    </Button>
  )
}

function EncabezadoPaciente({ consulta }: { consulta: Consulta }) {
  const { paciente, cita } = consulta
  const [nombre = "", apellido = ""] = cita.paciente.split(" ")
  const datos = [
    `DNI ${paciente.dni}`,
    paciente.edad !== null ? `${paciente.edad} años` : null,
    paciente.sexo ? (SEXOS[paciente.sexo] ?? paciente.sexo) : null,
    paciente.grupoSanguineo ? `Grupo ${paciente.grupoSanguineo}` : null,
  ].filter(Boolean)

  return (
    <Card>
      <CardContent className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex items-center gap-4">
          <Avatar className="size-14">
            <AvatarFallback className="bg-secondary text-lg font-semibold text-primary">
              {`${nombre[0] ?? ""}${apellido[0] ?? ""}`.toUpperCase()}
            </AvatarFallback>
          </Avatar>
          <div className="flex min-w-0 flex-col gap-1">
            <h1 className="text-2xl leading-8 font-semibold tracking-tight lg:text-[28px] lg:leading-9">
              {paciente.nombreCompleto}
            </h1>
            <p className="text-muted-foreground">{datos.join(" · ")}</p>
            {tieneAlergias(paciente.alergias) ? (
              <Badge variant="destructive" className="h-auto self-start py-1 text-sm whitespace-normal">
                Alergias: {paciente.alergias}
              </Badge>
            ) : null}
          </div>
        </div>
        <div className="flex flex-col gap-1 lg:items-end lg:text-right">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-semibold">{formatDiaHora(cita.fecha, cita.horaInicio)}</span>
            <EstadoCitaBadge estado={cita.estado} vista="doctor" />
          </div>
          <p className="text-muted-foreground">
            {consulta.especialidad} · Ticket {cita.numeroTicket}
          </p>
          <p className="text-muted-foreground">{cita.consultorio}</p>
        </div>
      </CardContent>
    </Card>
  )
}

/** Lo que corresponde hacer con la cita: atender, completar el reporte o solo consultar lo registrado. */
function Atencion({ consulta, idCita }: { consulta: Consulta; idCita: number }) {
  const { cita, modo } = consulta
  const registrada = consulta.historial.find((atencion) => atencion.idCita === idCita)

  if (consulta.puedeAtender) {
    return modo === "APOYO_DIAGNOSTICO" ? <ReporteExamenes cita={cita} /> : <FichaConsulta cita={cita} />
  }
  if (registrada) {
    return (
      <>
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <CircleCheckIcon className="size-5 text-success-foreground" aria-hidden="true" />
              Atención registrada
            </CardTitle>
            <CardDescription>
              {registrada.doctor} la registró el {formatFechaLarga(registrada.fecha).toLowerCase()}.
            </CardDescription>
          </CardHeader>
          <CardContent className="text-base">
            <DetalleAtencion atencion={registrada} />
          </CardContent>
        </Card>
        {modo === "APOYO_DIAGNOSTICO" && registrada.propia && registrada.resultados.length === 0 ? (
          <ReporteExamenes cita={cita} idAtencion={registrada.idAtencion} />
        ) : null}
      </>
    )
  }
  if (cita.estado === "NO_ASISTIO") {
    return (
      <Alert variant="warning">
        <UserXIcon />
        <AlertTitle>El paciente no asistió</AlertTitle>
        <AlertDescription>La cita quedó registrada como inasistencia.</AlertDescription>
      </Alert>
    )
  }
  if (cita.estado === "CANCELADA") {
    return (
      <Alert>
        <CircleAlertIcon />
        <AlertTitle>Cita cancelada</AlertTitle>
        <AlertDescription>Esta cita ya no está en tu agenda.</AlertDescription>
      </Alert>
    )
  }
  return (
    <Alert variant="brand">
      <CalendarClockIcon />
      <AlertTitle>La cita es el {formatFechaLarga(cita.fecha).toLowerCase()}</AlertTitle>
      <AlertDescription>
        Ese día podrás registrar la atención. Mientras tanto puedes revisar la historia clínica del paciente.
      </AlertDescription>
    </Alert>
  )
}

// Página donde el doctor atiende una cita: paciente, atención según su especialidad e historia clínica.
export function ConsultaDoctor({ idCita }: { idCita: number }) {
  const { data: consulta, error, isLoading, mutate: recargar } = useConsulta(idCita)

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6" role="status">
        <Skeleton className="h-28 rounded-xl" />
        <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(0,440px)]">
          <Skeleton className="h-96 rounded-xl" />
          <Skeleton className="h-96 rounded-xl" />
        </div>
        <span className="sr-only">Cargando la consulta…</span>
      </div>
    )
  }
  if (error || !consulta) {
    return (
      <div className="flex flex-col gap-4">
        <VolverAgenda />
        <Alert variant="destructive">
          <CircleAlertIcon />
          <AlertDescription>{error ? mensajeDeError(error) : "No encontramos esa cita en tu agenda."}</AlertDescription>
        </Alert>
      </div>
    )
  }

  const previas = consulta.historial.filter((atencion) => atencion.idCita !== idCita)

  return (
    <>
      <div className="flex flex-col gap-3">
        <VolverAgenda />
        <EncabezadoPaciente consulta={consulta} />
      </div>
      <div className="grid grid-cols-1 items-start gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(0,440px)]">
        <section aria-label="Atención de la cita" className="flex flex-col gap-6">
          {consulta.cita.motivo ? (
            <p className="text-base">
              <span className="font-medium">Motivo de la cita:</span> {consulta.cita.motivo}
            </p>
          ) : null}
          <Atencion consulta={consulta} idCita={idCita} />
        </section>
        <HistoriaClinica
          idCita={idCita}
          paciente={consulta.paciente}
          previas={previas}
          conAtenciones={consulta.historial.length > 0}
          onCambio={() => recargar()}
        />
      </div>
    </>
  )
}
