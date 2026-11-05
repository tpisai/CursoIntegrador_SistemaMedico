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
        <h1 className="text-[28px] leading-9 font-semibold tracking-tight lg:text-[32px] lg:leading-10">{titulo}</h1>
        <p className="text-base text-muted-foreground">{descripcion}</p>
      </div>
      {children}
    </div>
  )
}
