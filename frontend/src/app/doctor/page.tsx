import { redirect } from "next/navigation"

// El doctor trabaja desde su agenda: no tiene un panel de inicio aparte.
export default function DoctorPage() {
  redirect("/doctor/agenda")
}
