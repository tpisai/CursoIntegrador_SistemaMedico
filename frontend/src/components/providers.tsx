"use client"

import { SWRConfig } from "swr"

import { Toaster } from "@/components/ui/sonner"
import { fetcher } from "@/lib/api/http"

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <SWRConfig value={{ fetcher, shouldRetryOnError: false }}>
      {children}
      <Toaster theme="light" position="top-center" richColors />
    </SWRConfig>
  )
}
