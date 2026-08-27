import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Com globals:false o testing-library nao registra o auto-cleanup sozinho;
// sem isto, o DOM de um teste vaza para o seguinte.
afterEach(() => {
  cleanup()
})
