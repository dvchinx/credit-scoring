const decimal = new Intl.NumberFormat("es", { maximumFractionDigits: 2 });
const money = new Intl.NumberFormat("es", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const dateTime = new Intl.DateTimeFormat("es", { dateStyle: "medium", timeStyle: "short" });
const date = new Intl.DateTimeFormat("es", { dateStyle: "medium", timeZone: "UTC" });

export const formatNumber = (value: number) => decimal.format(value);
export const formatMoney = (value: number) => money.format(value);
export const formatPercent = (ratio: number, digits = 1) => `${(ratio * 100).toFixed(digits)}%`;
export const formatDateTime = (iso: string) => dateTime.format(new Date(iso));
/** Fechas sin hora (LocalDate del backend), interpretadas en UTC para no correrse un día. */
export const formatDate = (isoDate: string) => date.format(new Date(`${isoDate}T00:00:00Z`));

export function formatSigned(value: number, digits = 2): string {
  const fixed = value.toFixed(digits);
  return value > 0 ? `+${fixed}` : fixed;
}
