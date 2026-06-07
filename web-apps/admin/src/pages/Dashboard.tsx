import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { httpsCallable, type HttpsCallableResult } from "firebase/functions";
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  Users,
  UserPlus,
  MousePointerClick,
  Eye,
  TrendingUp,
} from "lucide-react";
import { functions } from "@/firebase";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

/**
 * Analytics dashboard — renders Firebase / GA4 engagement metrics fetched
 * via the `analyticsReport` callable (functions/src/analytics.ts). GA4 data
 * can't be read from the browser, so everything here comes through that
 * server-side proxy. See functions/README.md for the one-time GA setup.
 */

interface SeriesPoint {
  date: string;
  activeUsers: number;
  newUsers: number;
  sessions: number;
  screenPageViews: number;
}

interface AnalyticsReport {
  range: { days: number; startDate: string; endDate: string };
  rolling: { dau: number; wau: number; mau: number };
  totals: { newUsers: number; sessions: number; screenPageViews: number };
  series: SeriesPoint[];
}

const RANGES = [
  { days: 7, label: "7 days" },
  { days: 30, label: "30 days" },
  { days: 90, label: "90 days" },
];

const callReport = httpsCallable<{ days: number }, AnalyticsReport>(
  functions,
  "analyticsReport",
);

const numberFmt = new Intl.NumberFormat();

export const Dashboard = () => {
  const [days, setDays] = useState(30);

  const { data, isLoading, error } = useQuery({
    queryKey: ["analyticsReport", days],
    queryFn: async () => {
      const res: HttpsCallableResult<AnalyticsReport> = await callReport({
        days,
      });
      return res.data;
    },
    staleTime: 5 * 60 * 1000, // GA data lags ~hours; don't refetch aggressively
    retry: false,
  });

  return (
    <div className="flex flex-col gap-6 py-2">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Analytics</h1>
          <p className="text-sm text-muted-foreground">
            App engagement from Firebase Analytics (GA4).
          </p>
        </div>
        <div className="flex gap-1">
          {RANGES.map((r) => (
            <Button
              key={r.days}
              size="sm"
              variant={r.days === days ? "default" : "outline"}
              onClick={() => setDays(r.days)}
            >
              {r.label}
            </Button>
          ))}
        </div>
      </div>

      {error ? (
        <ErrorCard error={error} />
      ) : isLoading || !data ? (
        <DashboardSkeleton />
      ) : (
        <DashboardContent data={data} />
      )}
    </div>
  );
};

const DashboardContent = ({ data }: { data: AnalyticsReport }) => {
  const { rolling, totals } = data;
  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatCard
          icon={<Users className="size-4" />}
          label="Daily active users"
          hint="Active 1-day, as of yesterday"
          value={rolling.dau}
        />
        <StatCard
          icon={<Users className="size-4" />}
          label="Weekly active users"
          hint="Active 7-day, as of yesterday"
          value={rolling.wau}
        />
        <StatCard
          icon={<Users className="size-4" />}
          label="Monthly active users"
          hint="Active 28-day, as of yesterday"
          value={rolling.mau}
        />
        <StatCard
          icon={<UserPlus className="size-4" />}
          label="Downloads (new users)"
          hint={`New users over ${data.range.days} days · GA4 proxy for installs`}
          value={totals.newUsers}
        />
        <StatCard
          icon={<MousePointerClick className="size-4" />}
          label="Sessions"
          hint={`Over ${data.range.days} days`}
          value={totals.sessions}
        />
        <StatCard
          icon={<Eye className="size-4" />}
          label="Screen views"
          hint={`Over ${data.range.days} days`}
          value={totals.screenPageViews}
        />
      </div>

      <TrendCard
        title="Active users"
        description="Daily active users over the selected range."
        data={data.series}
        dataKey="activeUsers"
        color="var(--chart-1)"
      />
      <TrendCard
        title="Downloads (new users)"
        description="New users per day — GA4's closest proxy for app installs."
        data={data.series}
        dataKey="newUsers"
        color="var(--chart-3)"
      />
    </>
  );
};

const StatCard = ({
  icon,
  label,
  hint,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  hint: string;
  value: number;
}) => (
  <Card>
    <CardHeader className="pb-2">
      <CardDescription className="flex items-center gap-1.5">
        {icon}
        {label}
      </CardDescription>
      <CardTitle className="text-3xl tabular-nums">
        {numberFmt.format(value)}
      </CardTitle>
    </CardHeader>
    <CardContent>
      <p className="text-xs text-muted-foreground">{hint}</p>
    </CardContent>
  </Card>
);

const TrendCard = ({
  title,
  description,
  data,
  dataKey,
  color,
}: {
  title: string;
  description: string;
  data: SeriesPoint[];
  dataKey: keyof SeriesPoint;
  color: string;
}) => {
  const gradientId = `fill-${String(dataKey)}`;
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <TrendingUp className="size-4" />
          {title}
        </CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent>
        {data.length === 0 ? (
          <p className="py-12 text-center text-sm text-muted-foreground">
            No data for this range yet.
          </p>
        ) : (
          <ResponsiveContainer width="100%" height={260}>
            <AreaChart
              data={data}
              margin={{ top: 8, right: 12, left: 0, bottom: 0 }}
            >
              <defs>
                <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor={color} stopOpacity={0.4} />
                  <stop offset="95%" stopColor={color} stopOpacity={0.04} />
                </linearGradient>
              </defs>
              <CartesianGrid
                vertical={false}
                stroke="var(--border)"
                strokeDasharray="3 3"
              />
              <XAxis
                dataKey="date"
                tickFormatter={formatTick}
                tickLine={false}
                axisLine={false}
                fontSize={12}
                stroke="var(--muted-foreground)"
                minTickGap={24}
              />
              <YAxis
                tickLine={false}
                axisLine={false}
                fontSize={12}
                width={40}
                stroke="var(--muted-foreground)"
                allowDecimals={false}
              />
              <Tooltip
                contentStyle={{
                  background: "var(--popover)",
                  border: "1px solid var(--border)",
                  borderRadius: "var(--radius)",
                  fontSize: 12,
                  color: "var(--popover-foreground)",
                }}
                labelFormatter={(label) => formatTick(String(label))}
              />
              <Area
                type="monotone"
                dataKey={dataKey}
                stroke={color}
                strokeWidth={2}
                fill={`url(#${gradientId})`}
              />
            </AreaChart>
          </ResponsiveContainer>
        )}
      </CardContent>
    </Card>
  );
};

const DashboardSkeleton = () => (
  <>
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {Array.from({ length: 6 }).map((_, i) => (
        <Card key={i}>
          <CardHeader className="pb-2">
            <Skeleton className="h-4 w-28" />
            <Skeleton className="mt-2 h-8 w-20" />
          </CardHeader>
          <CardContent>
            <Skeleton className="h-3 w-40" />
          </CardContent>
        </Card>
      ))}
    </div>
    <Card>
      <CardHeader>
        <Skeleton className="h-5 w-32" />
      </CardHeader>
      <CardContent>
        <Skeleton className="h-[260px] w-full" />
      </CardContent>
    </Card>
  </>
);

const ErrorCard = ({ error }: { error: unknown }) => {
  const message = error instanceof Error ? error.message : String(error);
  const notConfigured = /GA4_PROPERTY_ID|not configured/i.test(message);
  return (
    <Card className="border-destructive/40">
      <CardHeader>
        <CardTitle className="text-base text-destructive">
          {notConfigured
            ? "Analytics not configured yet"
            : "Couldn't load analytics"}
        </CardTitle>
        <CardDescription>{message}</CardDescription>
      </CardHeader>
      {notConfigured && (
        <CardContent className="text-sm text-muted-foreground">
          Set <code>GA4_PROPERTY_ID</code>, enable the GA4 Data API, and grant
          the functions service account read access on the property — see{" "}
          <code>functions/README.md</code>.
        </CardContent>
      )}
    </Card>
  );
};

function formatTick(iso: string): string {
  // "2026-06-01" → "Jun 1"
  const [, m, d] = iso.split("-");
  if (!m || !d) return iso;
  const months = [
    "Jan",
    "Feb",
    "Mar",
    "Apr",
    "May",
    "Jun",
    "Jul",
    "Aug",
    "Sep",
    "Oct",
    "Nov",
    "Dec",
  ];
  return `${months[Number(m) - 1] ?? m} ${Number(d)}`;
}

export default Dashboard;
