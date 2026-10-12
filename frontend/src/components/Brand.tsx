/** Marca: un escudo (governance) con barras ascendentes (score). Solo decorativo. */
export function BrandMark({ className = "brand-mark" }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 32 32" aria-hidden="true">
      <defs>
        <linearGradient id="brand-gradient" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#6366f1" />
          <stop offset="1" stopColor="#10b981" />
        </linearGradient>
      </defs>
      <rect width="32" height="32" rx="9" fill="url(#brand-gradient)" />
      <rect x="8" y="17" width="4" height="7" rx="1.5" fill="#fff" opacity="0.7" />
      <rect x="14" y="13" width="4" height="11" rx="1.5" fill="#fff" opacity="0.85" />
      <rect x="20" y="8" width="4" height="16" rx="1.5" fill="#fff" />
    </svg>
  );
}

export function Brand() {
  return (
    <span className="brand">
      <BrandMark />
      <span>
        Credit Scoring
        <small>Decisiones explicables</small>
      </span>
    </span>
  );
}

/** Iniciales para avatares: "Luis Riesgo Medio" → "LR". */
export function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]!.toUpperCase())
    .join("");
}
