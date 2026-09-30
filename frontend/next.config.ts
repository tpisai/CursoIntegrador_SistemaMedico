import type { NextConfig } from "next"

// Dirección del backend de Spring Boot. Next.js reenvía /api/* hacia allí, así el
// navegador solo habla con el frontend y no hace falta configurar CORS.
const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080"

const nextConfig: NextConfig = {
  rewrites() {
    return [{ source: "/api/:ruta*", destination: `${BACKEND_URL}/api/:ruta*` }]
  },
}

export default nextConfig
