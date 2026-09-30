import { cn } from "@/lib/utils"

// Isotipo del prototipo (Figma 1:46): círculo blanco con cruz teal.
export function LogoMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" aria-hidden="true" className={cn("size-8 shrink-0", className)}>
      <circle cx="32" cy="32" r="32" fill="white" />
      <rect x="27" y="15" width="10" height="34" rx="4" className="fill-primary" />
      <rect x="15" y="27" width="34" height="10" rx="4" className="fill-primary" />
    </svg>
  )
}

export function Logo({ className, markClassName }: { className?: string; markClassName?: string }) {
  return (
    <span className={cn("inline-flex items-center gap-2.5 font-bold text-brand-foreground", className)}>
      <LogoMark className={markClassName} />
      SaludGrau
    </span>
  )
}
