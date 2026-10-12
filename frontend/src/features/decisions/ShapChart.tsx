import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  LabelList,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { useSyncExternalStore } from "react";
import { formatSigned } from "../../components/format";
import { featureLabel } from "./featureLabels";

export interface ShapBar {
  feature: string;
  value: number;
}

const INCREASES = "var(--risk-up)";
const DECREASES = "var(--risk-down)";
const TICK = { fill: "var(--muted)", fontSize: 12 };
const NARROW = "(max-width: 600px)";

/** En pantallas estrechas el eje de etiquetas se encoge para dejar sitio a las barras. */
function useNarrow(): boolean {
  return useSyncExternalStore(
    (onChange) => {
      if (typeof window.matchMedia !== "function") return () => {};
      const query = window.matchMedia(NARROW);
      query.addEventListener("change", onChange);
      return () => query.removeEventListener("change", onChange);
    },
    () => typeof window.matchMedia === "function" && window.matchMedia(NARROW).matches,
  );
}

/**
 * Barras divergentes en log-odds de default: a la derecha (rojo) lo que sube el riesgo, a la
 * izquierda (verde) lo que lo baja. El eje es simétrico y siempre incluye el 0, para que la
 * longitud de cada barra sea comparable. Es una vista del dato que calculó el backend.
 */
export function ShapChart({ bars, title }: { bars: ShapBar[]; title: string }) {
  const narrow = useNarrow();
  const yAxisWidth = narrow ? 112 : 190;
  const tick = narrow ? { ...TICK, fontSize: 10 } : TICK;
  const data = bars.map((bar) => ({ label: featureLabel(bar.feature), value: bar.value }));
  const extent = niceExtent(Math.max(0, ...data.map((d) => Math.abs(d.value))) * 1.25);
  const ticks = [-extent, -extent / 2, 0, extent / 2, extent];
  return (
    <figure className="chart" aria-label={title}>
      <ResponsiveContainer width="100%" height={Math.max(140, data.length * 34 + 40)}>
        <BarChart data={data} layout="vertical" margin={{ top: 4, right: 12, bottom: 4, left: 4 }} barCategoryGap={6}>
          <CartesianGrid horizontal={false} stroke="var(--border)" strokeDasharray="3 3" />
          <XAxis
            type="number"
            domain={[-extent, extent]}
            ticks={ticks}
            tickFormatter={(value: number) => (value === 0 ? "0" : formatSigned(value, extent < 0.4 ? 2 : 1))}
            tick={tick}
            axisLine={false}
            tickLine={false}
          />
          <YAxis type="category" dataKey="label" width={yAxisWidth} tick={tick} axisLine={false} tickLine={false} />
          <Tooltip cursor={{ fill: "var(--surface-2)" }} content={<ShapTooltip />} />
          <ReferenceLine x={0} stroke="var(--border-strong)" />
          <Bar dataKey="value" isAnimationActive={false} radius={4}>
            {data.map((entry) => (
              <Cell key={entry.label} fill={entry.value > 0 ? INCREASES : DECREASES} />
            ))}
            <LabelList dataKey="value" content={<BarValue />} />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
      <figcaption className="chart-legend" style={narrow ? undefined : { paddingLeft: yAxisWidth + 4, paddingRight: 12 }}>
        <span className="down">← baja el riesgo</span>
        <span className="up">sube el riesgo →</span>
      </figcaption>
    </figure>
  );
}

/** Extremo "redondo" del eje (0.2, 0.4, 0.6, 0.8, 1, 1.5, 2, 3…) para que los ticks sean legibles. */
function niceExtent(value: number): number {
  const steps = [0.1, 0.2, 0.4, 0.6, 0.8, 1, 1.5, 2, 3, 4, 5, 6, 8, 10];
  return steps.find((step) => step >= value) ?? Math.ceil(value);
}

interface LabelProps {
  x?: number | string;
  y?: number | string;
  width?: number | string;
  height?: number | string;
  value?: number | string;
}

function BarValue({ x = 0, y = 0, width = 0, height = 0, value = 0 }: LabelProps) {
  const numeric = Number(value);
  if (Math.abs(numeric) < 0.0005) return null;
  const start = Number(x);
  const end = start + Number(width);
  const positive = numeric > 0;
  return (
    <text
      x={positive ? Math.max(start, end) + 6 : Math.min(start, end) - 6}
      y={Number(y) + Number(height) / 2}
      dy="0.35em"
      textAnchor={positive ? "start" : "end"}
      fontSize={11}
      fontWeight={600}
      fill={positive ? INCREASES : DECREASES}
    >
      {formatSigned(numeric, 2)}
    </text>
  );
}

function ShapTooltip({ active, payload }: { active?: boolean; payload?: { payload: { label: string; value: number } }[] }) {
  if (!active || !payload?.length) return null;
  const { label, value } = payload[0]!.payload;
  return (
    <div className="chart-tooltip">
      <strong>{label}</strong>
      <div className="muted">
        {formatSigned(value, 3)} log-odds · {value > 0 ? "sube" : value < 0 ? "baja" : "no cambia"} el riesgo
      </div>
    </div>
  );
}
