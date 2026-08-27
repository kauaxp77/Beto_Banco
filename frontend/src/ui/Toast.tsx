import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from 'react'
import './ui.css'

interface ToastItem {
  id: number
  texto: string
  tipo: 'ok' | 'erro'
}

interface ToastApi {
  toast: (texto: string) => void
  toastErro: (texto: string) => void
}

const ToastContext = createContext<ToastApi | null>(null)

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast fora do ToastProvider')
  return ctx
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [itens, setItens] = useState<ToastItem[]>([])
  const proximo = useRef(1)

  const publicar = useCallback((texto: string, tipo: ToastItem['tipo']) => {
    const id = proximo.current++
    setItens((atual) => [...atual, { id, texto, tipo }])
    setTimeout(() => setItens((atual) => atual.filter((t) => t.id !== id)), 5000)
  }, [])

  const api = useMemo<ToastApi>(
    () => ({
      toast: (texto) => publicar(texto, 'ok'),
      toastErro: (texto) => publicar(texto, 'erro'),
    }),
    [publicar],
  )

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="bb-toasts" aria-live="polite">
        {itens.map((t) => (
          <div key={t.id} className={`bb-toast ${t.tipo === 'erro' ? 'bb-toast--erro' : ''}`.trim()}>
            {t.texto}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
