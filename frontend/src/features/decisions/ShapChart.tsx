import { Bar, BarChart, Cell, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { featureLabel } from "./featureLabels";

export interface ShapBar {
  feature: string;
  value: number;
}

const INCREASES = "var(--risk-up)";
const DECREASES = "var(--risk-down)";

/**
 * Barras divergentes en log-odds de default: a la derecha (rojo) lo que sube el riesgo, a la
 * izquierda (verde) lo que lo baja. Es una vista del dato que calculó el backend; no recalcula nada.
 */
export function ShapChart({ bars, title }: { bars: ShapBar[]; title: string }) {
  const data = bars.map((bar) => ({ label: featureLabel(bar.feature), value: bar.value }));
  return (
    <figure className="chart" aria-label={title}>
      <ResponsiveContainer width="100%" height={Math.max(160, data.length * 30 + 40)}>
        <BarChart data={data} layout="vertical" margin={{ top: 8, right: 24, bottom: 8, left: 8 }}>
          <XAxis type="number" tickFormatter={(value: number) => value.toFixed(2)} />
          <YAxis type="category" dataKey="label" width={230} tick={{ fontSize: 12 }} />
          <Tooltip formatter={(value) => Number(value).toFixed(3)} />
          <ReferenceLine x={0} stroke="var(--border-strong)" />
          <Bar dataKey="value" isAnimationActive={false}>
            {data.map((entry) => (
              <Cell key={entry.label} fill={entry.value > 0 ? INCREASES : DECREASES} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </figure>
  );
}
