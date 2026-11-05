# Etapa 1: dependencias. Solo se reinstalan si cambia package-lock.json.
FROM node:24-slim AS dependencias
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund


# Etapa 2: compila con output "standalone" (ver next.config.ts).
FROM node:24-slim AS compilacion
WORKDIR /app
COPY --from=dependencias /app/node_modules ./node_modules
COPY . .

# BACKEND_URL se usa aquí, al compilar: el destino de los rewrites queda escrito en la compilación.
# Cambiarlo en el contenedor ya creado no hace nada; hay que reconstruir la imagen.
ARG BACKEND_URL=http://backend:8080
ENV BACKEND_URL=$BACKEND_URL
ENV NEXT_TELEMETRY_DISABLED=1

# El proyecto aún no tiene carpeta public/; se crea para que el COPY de abajo no falle.
RUN npm run build && mkdir -p public


# Etapa 3: imagen final, solo con el servidor autocontenido. No lleva node_modules completo.
FROM node:24-slim
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
# Docker define HOSTNAME con el id del contenedor y server.js escucharía solo en esa dirección.
ENV HOSTNAME=0.0.0.0
ENV TZ=America/Lima

COPY --from=compilacion --chown=node:node /app/.next/standalone ./
COPY --from=compilacion --chown=node:node /app/.next/static ./.next/static
COPY --from=compilacion --chown=node:node /app/public ./public

USER node
EXPOSE 3000
CMD ["node", "server.js"]
