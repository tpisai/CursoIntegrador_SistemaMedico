import { cn } from "@/lib/utils"

// Isotipo del prototipo (Figma 1:46): círculo blanco con cruz teal; sobre fondo claro, los colores se invierten.
export function LogoMark({ className, sobreClaro = false }: { className?: string; sobreClaro?: boolean }) {
  const cruz = sobreClaro ? "fill-white" : "fill-primary"
  return (
    <svg viewBox="0 0 64 64" aria-hidden="true" className={cn("size-8 shrink-0", className)}>
      <circle cx="32" cy="32" r="32" className={sobreClaro ? "fill-primary" : "fill-white"} />
      <rect x="27" y="15" width="10" height="34" rx="4" className={cruz} />
      <rect x="15" y="27" width="34" height="10" rx="4" className={cruz} />
    </svg>
  )
}

export function Logo({
  className,
  markClassName,
  textoClassName,
  sobreClaro = false,
}: {
  className?: string
  markClassName?: string
  textoClassName?: string
  sobreClaro?: boolean
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-2.5 font-bold",
        sobreClaro ? "text-brand-strong" : "text-brand-foreground",
        className
      )}
    >
      <LogoMark className={markClassName} sobreClaro={sobreClaro} />
      <span className={textoClassName}>SaludGrau</span>
    </span>
  )
}
