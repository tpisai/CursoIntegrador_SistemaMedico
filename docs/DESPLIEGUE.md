# Despliegue de SaludGrau en producción

Guía para publicar el sistema en un servidor con dominio y HTTPS (EDT 6.1) y dejar respaldos diarios
(RNF-09). Está pensada para quien nunca ha desplegado: sigue los pasos en orden y copia los comandos.

> **Estado de estos archivos.** `docker-compose.yml` y los `Dockerfile` todavía **no se han ejecutado con
> Docker real**, porque en la PC donde se prepararon no estaba instalado. Lo que sí se probó está en la
> sección [Qué se probó](#qué-se-probó-y-qué-no). Por eso conviene hacer primero el
> [ensayo en una PC](#3-ensayo-en-una-pc-antes-de-pagar), antes de contratar nada.

## 1. Cómo queda el sistema

```
Internet ──HTTPS──> Caddy ──> frontend (Next.js) ──/api/*──> backend (Spring Boot) ──> PostgreSQL
                  80 y 443        puerto 3000                   puerto 8080             puerto 5432
```

- Todo corre en un solo servidor, cada pieza en su contenedor de Docker.
- Solo Caddy recibe tráfico de internet. El backend y la base no publican ningún puerto.
- Caddy obtiene y renueva solo el certificado SSL de Let's Encrypt.
- Los datos viven en dos volúmenes que sobreviven a reinicios y actualizaciones: `postgres_datos` (la
  base) y `archivos` (los documentos médicos).

**Cuidado:** `docker compose down -v` borra esos volúmenes, es decir, toda la información. Sin la `-v`
no se borra nada. No la uses salvo donde esta guía lo indica.

## 2. Elegir servidor y dominio

**Presupuesto del acta:** hosting y dominio S/ 170 al año, y servidor cloud S/ 1,070 al año. Con un VPS no
se paga hosting aparte, así que los S/ 170 quedan para el dominio.

**Qué necesita el sistema:** Ubuntu LTS y 2 GB de RAM como mínimo; se recomiendan 4 GB porque las imágenes se
compilan en el propio servidor. Como referencia, medido en Windows con una sola persona usando el sistema, el
backend llegó a 560 MB y el frontend a unos 90 MB, sin contar PostgreSQL. No se midió la RAM que consume
compilar las imágenes.

**Requisito que pesa más de lo que parece:** los recordatorios (RF-22) y la recuperación de PIN salen por
SMTP. Si el proveedor bloquea el puerto 587, Gmail no funciona desde ese servidor.

Precios leídos en las páginas oficiales el **5 de noviembre de 2026**. Pueden cambiar: Hetzner los subió
durante 2026. No incluyen impuestos.

| | Hetzner Cloud (Europa) | DigitalOcean | Oracle Cloud Always Free |
| --- | --- | --- | --- |
| Plan | CPX22: 2 vCPU, 4 GB, 80 GB | Basic: 2 vCPU, 4 GiB, 80 GiB | Ampere A1 (ARM): hasta 2 OCPU, 12 GB, 200 GB |
| Precio | €19.99 al mes | US$ 24 al mes | Gratis |
| Al año, aproximado | S/ 937 | S/ 998 | S/ 0 |
| Plan de 2 GB | CPX12, 2 vCPU: €11.99 (S/ 562 al año) | 1 vCPU: US$ 12 (S/ 499 al año) | No aplica |
| Puerto SMTP 587 | Abierto (bloquea 25 y 465) | **Bloqueado** (25, 465 y 587) | No lo pude verificar |
| Respaldo del proveedor | No revisado | Diario: +30 % del precio | 5 copias de volumen incluidas |
| Principal riesgo | Servidor lejos de Perú; latencia no medida | El correo actual no saldría | Oracle puede reclamar instancias inactivas |

Los soles se calcularon con el tipo de cambio venta del BCRP del 2 de noviembre de 2026 (S/ 3.466 por dólar
y S/ 3.907 por euro). El euro varió entre 3.86 y 4.06 en esas dos semanas, así que tómalos como aproximados.

**Recomendación: Hetzner CPX22 en Europa.** Es la opción de pago que cabe en el presupuesto y deja salir el
correo por el 587 sin cambiar código. Antes de pagar, confirma tres cosas:

1. **Disponibilidad.** Al leer la página, los botones de crear servidor decían "currently unavailable".
   Puede ser un efecto de cómo se leyó la página, pero hay que confirmarlo en la consola de Hetzner.
2. **Impuestos.** Si se sumara un 18 % al precio, el CPX22 pasaría a unos S/ 1,106 y se saldría por poco del
   presupuesto. No se revisó qué impuesto aplica. En ese caso usa el CPX12 con la memoria de intercambio del
   paso 4.
3. **Latencia.** Los planes de Hetzner en Estados Unidos están más cerca, pero cuestan más: el de 4 GB son
   €32.49 al mes (unos S/ 1,523 al año, fuera de presupuesto) y el de 2 GB, €17.99 (unos S/ 843).

**Otras opciones.** DigitalOcean solo conviene si el equipo acepta contratar un servicio de correo
transaccional que funcione por otro puerto, lo que no se revisó. Oracle sirve para ensayar sin pagar: las
cinco imágenes base tienen versión ARM y `package-lock.json` incluye los paquetes ARM. Para producción es
arriesgado, porque una instancia con poco uso durante 7 días puede ser reclamada, y SaludGrau tiene poco
tráfico. Vultr también deja abierto el 587, pero su página de precios no se pudo leer: **su precio no está
verificado**.

**Dominio.** Un blog peruano de hosting indica unos S/ 110 el primer año para un `.pe` y S/ 120 a 150 al
renovar. **No lo pude confirmar en [punto.pe](https://punto.pe)**: revisa ahí el precio real, renovación
incluida, antes de comprar.

Fuentes: [Hetzner, precios](https://www.hetzner.com/cloud/regular-performance) y
[puertos](https://docs.hetzner.com/cloud/servers/faq/);
[DigitalOcean, precios](https://www.digitalocean.com/pricing/droplets) y
[SMTP](https://docs.digitalocean.com/support/why-is-smtp-blocked/);
[Oracle Always Free](https://docs.oracle.com/en-us/iaas/Content/FreeTier/resourceref.htm);
[Vultr, SMTP](https://docs.vultr.com/support/products/compute/why-is-smtp-blocked);
[BCRP, tipo de cambio](https://estadisticas.bcrp.gob.pe).

## 3. Ensayo en una PC antes de pagar

Con [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado, desde la raíz del proyecto:

```bash
cp .env.example .env
```

Edita `.env`: pon cualquier clave en `POSTGRES_PASSWORD`, una clave de 32 caracteres o más en
`SALUDGRAU_TOKEN_SECRETO` y **borra la línea `COMPOSE_PROFILES=ssl`**. Si ya tienes el sistema corriendo en
esa PC, detén antes el `npm run dev`, porque usa el mismo puerto 3000. Luego:

```bash
docker compose up -d --build
docker compose ps
```

Cuando los tres servicios digan `healthy`, abre <http://127.0.0.1:3000> y entra con una cuenta de prueba. Si
algo falla aquí, arréglalo antes de contratar. Para borrar el ensayo por completo: `docker compose down -v`.

## 4. Preparar el servidor

Crea el servidor con **Ubuntu 24.04 LTS** y agrega tu llave SSH al crearlo. Entra desde PowerShell, usando
la IP que te dé el proveedor:

```bash
ssh root@IP_DEL_SERVIDOR
```

Crea un usuario propio, deja la hora de Lima y activa el firewall:

```bash
adduser saludgrau
usermod -aG sudo saludgrau
rsync --archive --chown=saludgrau:saludgrau ~/.ssh /home/saludgrau
timedatectl set-timezone America/Lima
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 443/udp
ufw enable
```

Sal con `exit` y vuelve a entrar con `ssh saludgrau@IP_DEL_SERVIDOR`.

Instala Docker desde su [repositorio oficial](https://docs.docker.com/engine/install/ubuntu/). Docker
desaconseja su script rápido `get.docker.com` para producción.

```bash
sudo apt update
sudo apt install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker saludgrau
```

Sal y vuelve a entrar para que el último comando haga efecto, y comprueba con `docker run hello-world`.

**Solo si el servidor tiene 2 GB de RAM,** agrega memoria de intercambio para que la compilación no se quede
sin memoria:

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

## 5. Apuntar el dominio

En el panel donde compraste el dominio crea un registro **A**: nombre `@` (o el subdominio que quieras) y
valor la IP del servidor. Hazlo **antes** de levantar el sistema, porque Caddy necesita que el dominio ya
apunte al servidor para obtener el certificado. Puede tardar desde minutos hasta horas. Compruébalo:

```bash
nslookup tu-dominio.pe
```

Debe mostrar la IP del servidor.

## 6. Bajar el proyecto y configurarlo

```bash
sudo mkdir -p /opt/saludgrau
sudo chown saludgrau:saludgrau /opt/saludgrau
git clone https://github.com/tpisai/CursoIntegrador_SistemaMedico.git /opt/saludgrau
cd /opt/saludgrau
git checkout LA_RAMA_ACORDADA
ls sql
```

**Revisa la rama.** `sql/` debe mostrar `04_recuperacion_pin.sql` y `05_profesionales_cmi.sql`. Al escribir
esta guía esos dos scripts estaban en `mejoras-cierre` y todavía no en `main`. La primera carga usa los que
encuentre, así que desde `main` solo cargaría tres.

Crea las claves. Ejecuta este comando dos veces y guarda cada resultado:

```bash
openssl rand -hex 32
```

Crea el archivo de configuración:

```bash
cp .env.example .env
chmod 600 .env
nano .env
```

Cambia estos valores. Guarda con `Ctrl+O`, `Enter`, y sal con `Ctrl+X`.

| Variable | Valor |
| --- | --- |
| `POSTGRES_PASSWORD` | La primera clave generada. Que sea nueva, no la de tu PC |
| `SALUDGRAU_TOKEN_SECRETO` | La segunda clave generada |
| `DOMINIO` | Tu dominio, por ejemplo `saludgrau.pe` |

Si `SALUDGRAU_TOKEN_SECRETO` falta, tiene menos de 32 caracteres o es la clave de desarrollo, el backend no
arranca. Es a propósito: con esa clave cualquiera podría fabricar una sesión de administrador.

**Correo.** Para que salga de verdad, quita el `#` de las líneas `SPRING_MAIL_*` y complétalas. Con Gmail: host
`smtp.gmail.com`, puerto `587` y una
[contraseña de aplicación](https://support.google.com/accounts/answer/185833), no la de la cuenta; para crearla
hay que tener activa la verificación en dos pasos. Si no lo configuras, el sistema funciona igual y los correos
solo se registran en el log. No dejes
`SPRING_MAIL_HOST=` vacía: con la variable definida sin valor, los envíos fallan en lugar de registrarse.

Guarda una copia de `.env` en un lugar seguro. Si se pierde, no hay otra.

## 7. Levantar el sistema

```bash
docker compose up -d --build
```

La primera vez tarda varios minutos, porque compila el backend y el frontend. Al terminar:

```bash
docker compose ps
```

`postgres`, `backend` y `frontend` deben decir `healthy` y `caddy`, `Up`. Para ver qué pasa en cada uno:

```bash
docker compose logs -f backend    # Ctrl+C para salir, el sistema sigue corriendo
```

Confirma que la primera carga usó todos los scripts:

```bash
docker compose logs postgres | grep "Se cargará"
```

## 8. Qué revisar después de desplegar

**Funcionamiento**

- [ ] `https://tu-dominio.pe` abre con candado y `http://tu-dominio.pe` redirige a HTTPS.
- [ ] `https://tu-dominio.pe/api/especialidades` devuelve una lista en JSON.
- [ ] Se puede iniciar sesión como paciente, reservar una cita y descargar el ticket en PDF.
- [ ] Un doctor puede adjuntar un documento y el paciente lo descarga.
- [ ] Tras `docker compose restart`, ese documento y la cita siguen ahí.
- [ ] `docker compose exec backend date` muestra la hora de Lima.
- [ ] Si configuraste correo, llega un recordatorio. Si no llega, busca `No se pudo enviar` en el log del backend.

**Seguridad**

- [ ] Desde tu PC, en PowerShell, estos puertos deben dar `TcpTestSucceeded : False`:

  ```powershell
  Test-NetConnection tu-dominio.pe -Port 5432
  Test-NetConnection tu-dominio.pe -Port 8080
  Test-NetConnection tu-dominio.pe -Port 3000
  ```

- [ ] **Cuentas de prueba, lo más importante de esta lista.** Los datos iniciales crean cuentas con PIN
  conocido, y están publicados en el README de un repositorio público: cualquiera podría entrar como
  administración con ellos. Antes de compartir la dirección, cambia el PIN de administración (DNI
  `44556677`). Elige uno de 6 dígitos y escríbelo en lugar de `PIN_NUEVO`:

  ```bash
  docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
  UPDATE usuario SET pin_hash = crypt('PIN_NUEVO', gen_salt('bf')) WHERE dni = '44556677';
  SQL
  ```

  Si el sistema es para uso real y no para una demostración, desactiva además las demás cuentas de prueba:

  ```bash
  docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
  UPDATE usuario SET estado = 'INACTIVO' WHERE dni <> '44556677';
  SQL
  ```

- [ ] **Registro de doctores.** Hoy cualquier persona puede crearse una cuenta de doctor desde el login, sin
  verificación. El equipo debe decidir si eso es aceptable en un servidor público.
- [ ] **Datos reales.** Son datos de salud. Antes de registrar pacientes reales, el equipo debe revisar qué exige
  la Ley 29733 de protección de datos personales. Esta guía no cubre ese análisis.
- [ ] Los respaldos están programados y ya probaste restaurar uno (siguiente sección).

## 9. Respaldos diarios (RNF-09)

`sql/respaldar_docker.sh` guarda en `respaldos/AAAA-MM-DD_HHMMSS/` la base (`centro_salud.dump`) y los
documentos (`archivos.tar.gz`), y conserva los 7 más recientes. Si un respaldo falla, no borra los anteriores.

Pruébalo una vez a mano:

```bash
cd /opt/saludgrau
bash sql/respaldar_docker.sh
ls -la respaldos
```

Prográmalo cada día a las 3:30 a. m. Abre el programador de tareas con `crontab -e` y agrega esta línea:

```
30 3 * * * cd /opt/saludgrau && bash sql/respaldar_docker.sh >> /home/saludgrau/respaldo.log 2>&1
```

Al día siguiente revisa `/home/saludgrau/respaldo.log`.

**Un respaldo que solo está en el servidor no protege si el servidor se pierde.** Baja una copia al menos una
vez por semana desde tu PC y guárdala en un lugar privado, porque contiene datos de pacientes:

```powershell
scp -r saludgrau@IP_DEL_SERVIDOR:/opt/saludgrau/respaldos D:\respaldos-saludgrau
```

### Restaurar en el servidor

Esto reemplaza los datos actuales: se pierde lo registrado después del respaldo.

```bash
cd /opt/saludgrau
FECHA=2026-11-05_033000          # la carpeta que quieres restaurar
docker compose stop frontend backend
docker compose exec -T postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner' < respaldos/$FECHA/centro_salud.dump
docker compose start backend
docker compose ps                # espera a que backend diga healthy
docker compose exec -T backend tar -C /app/archivos -xzf - < respaldos/$FECHA/archivos.tar.gz
docker compose start frontend
```

### Comprobar un respaldo sin tocar la base real

Hazlo una vez al mes. Un respaldo que nunca se probó no se sabe si sirve.

```bash
docker compose exec -T postgres sh -c 'createdb -U "$POSTGRES_USER" prueba_restauracion'
docker compose exec -T postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d prueba_restauracion --no-owner' < respaldos/$FECHA/centro_salud.dump
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d prueba_restauracion -c "SELECT count(*) FROM usuario"'
docker compose exec -T postgres sh -c 'dropdb -U "$POSTGRES_USER" prueba_restauracion'
```

### En una PC con Windows

Para el PostgreSQL local está `sql\respaldar_bd.ps1`, que hace lo mismo y lee la contraseña de
`citas_salud/config/application.properties`. Los pasos de restauración están al inicio del script.

```powershell
powershell -ExecutionPolicy Bypass -File sql\respaldar_bd.ps1
```

Para que corra solo, créale una tarea diaria en el Programador de tareas de Windows con ese mismo comando.

## 10. Publicar una versión nueva

Primero haz un respaldo y baja los cambios:

```bash
cd /opt/saludgrau
bash sql/respaldar_docker.sh
git pull
```

La base **no** se actualiza sola, porque la primera carga solo corre con el volumen vacío. Si la versión trae un
script SQL nuevo, aplícalo a mano en este momento, porque el backend no arranca si le falta una tabla. Por
ejemplo:

```bash
docker compose exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < sql/04_recuperacion_pin.sql
```

Después reconstruye y reinicia. Los volúmenes no se tocan:

```bash
docker compose up -d --build
```

## 11. Problemas comunes

| Qué ves | Causa probable | Qué hacer |
| --- | --- | --- |
| `Define POSTGRES_PASSWORD en .env` | Falta `.env` o esa línea | Revisa el paso 6 |
| `Could not resolve placeholder 'SALUDGRAU_TOKEN_SECRETO'` | La variable no llega al backend | Revisa `.env` y ejecuta `docker compose up -d` |
| `SALUDGRAU_TOKEN_SECRETO debe ser una clave propia` | Clave corta o de ejemplo | Genera una con `openssl rand -hex 32` |
| `Schema validation: missing table` | Falló la primera carga, o falta un script SQL en una actualización | Lee `docker compose logs postgres`. Si es la primera carga y no hay datos reales, corrige y ejecuta `docker compose down -v` y `docker compose up -d --build` |
| `password authentication failed` | Cambiaste `POSTGRES_PASSWORD` después de crear el volumen | Esa clave solo se aplica la primera vez: vuelve a poner la original en `.env` |
| La página abre pero `/api/...` da error 500 | El frontend se compiló con otro `BACKEND_URL` | Corrige `.env` y ejecuta `docker compose up -d --build`. Reiniciar no basta: esa dirección queda fija al compilar |
| El navegador avisa que el sitio no es seguro | El dominio aún no apunta al servidor, o los puertos 80 y 443 están cerrados | Revisa `docker compose logs caddy` y el paso 5. No borres el volumen `caddy_datos` para reintentar: Let's Encrypt limita las solicitudes |
| `No se pudo enviar el correo` en el log | Credenciales incorrectas o puerto bloqueado por el proveedor | Revisa las variables `SPRING_MAIL_*` y el puerto 587 |
| La compilación se corta con `exit code 137` | Falta memoria | Agrega la memoria de intercambio del paso 4 |

## Qué se probó y qué no

**Probado de verdad**, sin Docker, con PostgreSQL 17 en una instancia temporal, el jar real con el perfil `prod`
y el `server.js` compilado montado como en la imagen final:

- La primera carga con 3 y con 5 scripts, y que un fallo a medias deja la base vacía.
- Que el backend no arranca sin claves o con claves de ejemplo, y que `prod` no imprime SQL.
- La cadena frontend, backend y base: inicio de sesión, ticket PDF, subida y descarga de un documento de
  4.9 MB y rechazo de uno de 6 MB.
- Que el frontend no relee `BACKEND_URL` al arrancar.
- Los comandos SQL de esta guía para cambiar el PIN y desactivar cuentas.
- `respaldar_bd.ps1` completo, y la lógica de `respaldar_docker.sh` con un `docker` simulado.
- La restauración con `pg_restore`, también recibiendo el respaldo por tubería como en el servidor.

**Sin probar:** la construcción de las imágenes, el arranque de los contenedores, los healthchecks, los
permisos de los volúmenes, Caddy y el certificado, el envío de un correo real, la compilación en un servidor
con poca memoria y todos los pasos de las secciones 4 a 7. `docker-compose.yml` solo se validó contra el
esquema oficial de Compose y los `Dockerfile` con un linter.
