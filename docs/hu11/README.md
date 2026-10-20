# HU11 — Contrato compartido

El payload de cada QR está definido en [`qr-contract.json`](qr-contract.json). Su copia del frontend debe
ser idéntica; la app usa los casos válidos e inválidos para probar el parser.

Formato actual: `orquideamobileapp://species/{uuid}`. El endpoint de descarga toma el UUID de una ficha
existente y produce un PNG cuadrado de 1024 × 1024, sin persistir archivos ni añadir migraciones.
La operación requiere administrador/superadministrador; el visitante consulta la ficha mediante el
endpoint público existente.

La compilación e integración real de este worktree todavía dependen de Maven, Java 21 y Docker; consultar
[validation.md](validation.md) antes de dar por aprobado el backend.
