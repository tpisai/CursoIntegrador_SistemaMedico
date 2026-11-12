"use client"

import { useState } from "react"
import { ClipboardListIcon, PencilIcon } from "lucide-react"

import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { AntecedentesDialog } from "@/components/consulta/antecedentes-dialog"
import { DetalleAtencion } from "@/components/consulta/detalle-atencion"
import type { AtencionPrevia, PacienteConsulta } from "@/lib/api/types"
import { formatFecha } from "@/lib/format"

function Fila({ etiqueta, valor }: { etiqueta: string; valor: string | null }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-sm font-medium text-muted-foreground">{etiqueta}</dt>
      <dd className={valor ? "whitespace-pre-line" : "text-muted-foreground"}>{valor ?? "Sin registrar"}</dd>
    </div>
  )
}

function plural(cantidad: number, uno: string, varios: string) {
  return `${cantidad} ${cantidad === 1 ? uno : varios}`
}

/** Expediente del paciente: antecedentes, datos de contacto y atenciones anteriores con sus documentos. */
export function HistoriaClinica({
  idCita,
  paciente,
  previas,
  onCambio,
}: {
  idCita: number
  paciente: PacienteConsulta
  // Atenciones anteriores a la cita que se está viendo, de la más reciente a la más antigua.
  previas: AtencionPrevia[]
  onCambio: () => Promise<unknown>
}) {
  const [editando, setEditando] = useState(false)
  const documentos = previas.reduce((total, atencion) => total + atencion.documentos.length, 0)
  const emergencia = [paciente.contactoEmergencia, paciente.telefonoEmergencia].filter(Boolean).join(" · ")

  return (
    <aside aria-label="Historia clínica del paciente" className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle>Antecedentes</CardTitle>
          <CardDescription>
            {paciente.numeroHistoria ? `Historia clínica ${paciente.numeroHistoria}` : "Sin historia clínica abierta"}
          </CardDescription>
          {paciente.numeroHistoria ? (
            <CardAction>
              <Button variant="outline" className="h-11 sm:h-8" onClick={() => setEditando(true)}>
                <PencilIcon data-icon="inline-start" />
                Editar
              </Button>
            </CardAction>
          ) : null}
        </CardHeader>
        <CardContent>
          <dl className="flex flex-col gap-3">
            <Fila etiqueta="Alergias" valor={paciente.alergias} />
            <Fila etiqueta="Antecedentes y enfermedades" valor={paciente.antecedentes} />
            <Fila etiqueta="Medicación habitual" valor={paciente.medicacionHabitual} />
          </dl>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Atenciones anteriores</CardTitle>
          <CardDescription>
            {previas.length
              ? `${plural(previas.length, "atención", "atenciones")} y ${plural(documentos, "documento", "documentos")} en todo el centro de salud.`
              : "Aún no tiene atenciones registradas."}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {previas.length ? (
            <Accordion type="multiple">
              {previas.map((atencion) => (
                <AccordionItem key={atencion.idAtencion} value={String(atencion.idAtencion)}>
                  <AccordionTrigger className="gap-3 py-3 hover:no-underline">
                    <span className="flex min-w-0 flex-col gap-1">
                      <span className="flex flex-wrap items-center gap-2 text-base">
                        {formatFecha(atencion.fecha)}
                        <Badge variant={atencion.tipoEspecialidad === "APOYO_DIAGNOSTICO" ? "secondary" : "outline"}>
                          {atencion.especialidad}
                        </Badge>
                      </span>
                      <span className="font-normal text-muted-foreground">
                        {atencion.doctor}
                        {atencion.diagnostico ? `: ${atencion.diagnostico}` : ""}
                      </span>
                    </span>
                  </AccordionTrigger>
                  <AccordionContent className="pb-4 text-base">
                    <DetalleAtencion atencion={atencion} />
                  </AccordionContent>
                </AccordionItem>
              ))}
            </Accordion>
          ) : (
            <Empty className="border border-dashed p-6">
              <EmptyHeader>
                <EmptyMedia variant="icon">
                  <ClipboardListIcon />
                </EmptyMedia>
                <EmptyTitle>Primera atención</EmptyTitle>
                <EmptyDescription>Lo que registres hoy abrirá su historial en el centro de salud.</EmptyDescription>
              </EmptyHeader>
            </Empty>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Datos de contacto</CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="flex flex-col gap-3">
            <Fila etiqueta="Teléfono" valor={paciente.telefono} />
            <Fila etiqueta="Correo" valor={paciente.correo} />
            <Fila etiqueta="Dirección" valor={paciente.direccion} />
            <Fila etiqueta="Contacto de emergencia" valor={emergencia || null} />
          </dl>
        </CardContent>
      </Card>

      <AntecedentesDialog
        idCita={idCita}
        paciente={paciente}
        open={editando}
        onOpenChange={setEditando}
        onGuardado={onCambio}
      />
    </aside>
  )
}
