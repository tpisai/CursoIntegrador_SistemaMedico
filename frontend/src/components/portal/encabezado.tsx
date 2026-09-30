export function Encabezado({
  titulo,
  descripcion,
  children,
}: {
  titulo: React.ReactNode
  descripcion: string
  children?: React.ReactNode
}) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-2xl font-bold tracking-tight">{titulo}</h1>
        <p className="text-sm text-muted-foreground">{descripcion}</p>
      </div>
      {children}
    </div>
  )
}
