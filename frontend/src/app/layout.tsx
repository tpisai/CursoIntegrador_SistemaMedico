import type { Metadata } from "next"
import { Geist_Mono, Inter } from "next/font/google"

import { Providers } from "@/components/providers"
import "./globals.css"

// El prototipo de Figma usa Inter.
const inter = Inter({
  variable: "--font-sans",
  subsets: ["latin"],
})

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
})

export const metadata: Metadata = {
  title: {
    default: "SaludGrau · Centro de Salud Miguel Grau",
    template: "%s · SaludGrau",
  },
  description:
    "Reserva tus citas médicas en línea y consulta tu historial en el Centro de Salud Miguel Grau de Chaclacayo.",
}

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="es" className={`${inter.variable} ${geistMono.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col">
        <Providers>{children}</Providers>
      </body>
    </html>
  )
}
