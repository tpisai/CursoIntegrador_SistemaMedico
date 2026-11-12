"use client"

import { useState } from "react"
import { DownloadIcon, FileTextIcon } from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Spinner } from "@/components/ui/spinner"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { descargarDocumentoDePaciente } from "@/lib/api/consulta"
import { descargarReceta } from "@/lib/api/doctor"
import { mensajeDeError } from "@/lib/api/http"
import type { AtencionPrevia, MedicamentoRecetado } from "@/lib/api/types"
import { guardarArchivo } from "@/lib/descargar"

/** "1 tableta · cada 8 horas · por 3 días · vía oral · total 9 tabletas" con los datos que haya. */
function indicacion(m: MedicamentoRecetado) {
  return [
    m.unidadesPorToma,
    m.frecuencia?.toLowerCase(),
    m.duracion ? `por ${m.duracion.toLowerCase()}` : null,
    m.viaAdministracion ? `vía ${m.viaAdministracion.toLowerCase()}` : null,
    m.cantidad ? `total ${m.cantidad}` : null,
  ]
    .filter(Boolean)
    .join(" · ")
}

function Dato({ etiqueta, valor }: { etiqueta: string; valor: string | null }) {
  if (!valor) return null
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-sm font-medium text-muted-foreground">{etiqueta}</dt>
      <dd className="whitespace-pre-line">{valor}</dd>
    </div>
  )
}

/** Lo que se registró en una atención: ficha o conclusión, receta, resultados de exámenes y documentos. */
export function DetalleAtencion({ atencion }: { atencion: AtencionPrevia }) {
  const [descargando, setDescargando] = useState<string | null>(null)
  const esExamen = atencion.tipoEspecialidad === "APOYO_DIAGNOSTICO"

  async function descargar(clave: string, obtener: () => Promise<Blob>, nombre: string) {
    setDescargando(clave)
    try {
      guardarArchivo(await obtener(), nombre)
    } catch (error) {
      toast.error(mensajeDeError(error))
    } finally {
      setDescargando(null)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <dl className="flex flex-col gap-3">
        <Dato etiqueta={esExamen ? "Examen solicitado" : "Síntomas"} valor={atencion.anamnesis} />
        <Dato etiqueta="Exploración física" valor={atencion.examenFisico} />
        <Dato etiqueta={esExamen ? "Conclusión" : "Diagnóstico"} valor={atencion.diagnostico} />
        <Dato etiqueta="Tratamiento" valor={atencion.tratamiento} />
        <Dato etiqueta="Observaciones" valor={atencion.observaciones} />
      </dl>

      {atencion.resultados.length ? (
        <div className="flex flex-col gap-2">
          <h4 className="text-sm font-medium text-muted-foreground">Resultados de exámenes</h4>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Examen</TableHead>
                <TableHead>Resultado</TableHead>
                <TableHead>Referencia</TableHead>
                <TableHead>Interpretación</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {atencion.resultados.map((r, i) => (
                <TableRow key={i}>
                  <TableCell className="font-medium whitespace-normal">{r.tipoExamen}</TableCell>
                  <TableCell className="whitespace-normal tabular-nums">{r.resultado}</TableCell>
                  <TableCell className="whitespace-normal text-muted-foreground">{r.valorReferencia ?? "—"}</TableCell>
                  <TableCell className="whitespace-normal">{r.interpretacion ?? "—"}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      ) : null}

      {atencion.medicamentos.length ? (
        <div className="flex flex-col gap-2">
          <h4 className="text-sm font-medium text-muted-foreground">Receta</h4>
          <ul className="flex flex-col gap-2">
            {atencion.medicamentos.map((m, i) => (
              <li key={i} className="flex flex-col">
                <span className="font-medium">
                  {[m.nombreMedicamento, m.dosis].filter(Boolean).join(" ")}
                  {m.presentacion ? ` (${m.presentacion.toLowerCase()})` : ""}
                </span>
                {indicacion(m) ? <span className="text-muted-foreground">{indicacion(m)}</span> : null}
              </li>
            ))}
          </ul>
          {atencion.propia ? (
            <Button
              variant="outline"
              className="h-11 self-start"
              disabled={descargando === "receta"}
              onClick={() =>
                descargar("receta", () => descargarReceta(atencion.idAtencion), `receta-${atencion.idAtencion}.pdf`)
              }
            >
              {descargando === "receta" ? <Spinner data-icon="inline-start" /> : <DownloadIcon data-icon="inline-start" />}
              Descargar receta (PDF)
            </Button>
          ) : null}
        </div>
      ) : null}

      {atencion.documentos.length ? (
        <div className="flex flex-col gap-2">
          <h4 className="text-sm font-medium text-muted-foreground">Documentos</h4>
          <ul className="flex flex-col gap-2">
            {atencion.documentos.map((doc) => {
              const clave = `doc-${doc.idDocumento}`
              return (
                <li
                  key={doc.idDocumento}
                  className="flex items-center justify-between gap-3 rounded-lg bg-muted/60 py-1 pr-1 pl-3"
                >
                  <span className="flex min-w-0 items-center gap-2">
                    <FileTextIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                    <span className="truncate">{doc.tipoDocumento}</span>
                  </span>
                  <Button
                    variant="ghost"
                    className="h-11 text-primary sm:h-8"
                    disabled={descargando === clave}
                    onClick={() =>
                      descargar(clave, () => descargarDocumentoDePaciente(doc.idDocumento), doc.nombreArchivo)
                    }
                  >
                    {descargando === clave ? <Spinner data-icon="inline-start" /> : <DownloadIcon data-icon="inline-start" />}
                    Descargar
                    <span className="sr-only"> {doc.tipoDocumento}</span>
                  </Button>
                </li>
              )
            })}
          </ul>
        </div>
      ) : null}
    </div>
  )
}
