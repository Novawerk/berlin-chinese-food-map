import type { Metadata, Viewport } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { Providers } from "./providers";
import { Navbar } from "@/components/Navbar";
import { Footer } from "@/components/Footer";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  metadataBase: new URL("https://berlinfoodmap.novawerk.io"),
  title: "Berlin Chinese Food Map | 柏林中餐地图",
  description:
    "Community-driven guide to the best Chinese restaurants in Berlin. Discover authentic Chinese cuisine with map view, smart filters, and bilingual support.",
  openGraph: {
    title: "Berlin Chinese Food Map | 柏林中餐地图",
    description:
      "Community-driven guide to the best Chinese restaurants in Berlin.",
    url: "https://berlinfoodmap.novawerk.io",
    siteName: "Berlin Chinese Food Map",
    images: [{ url: "/logo.png", width: 1024, height: 1024, alt: "PinWo — Berlin Chinese Food Map" }],
    locale: "en_US",
    type: "website",
  },
  twitter: {
    card: "summary",
    title: "Berlin Chinese Food Map | 柏林中餐地图",
    description:
      "Community-driven guide to the best Chinese restaurants in Berlin.",
    images: ["/logo.png"],
  },
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#FFFCF8" },
    { media: "(prefers-color-scheme: dark)", color: "#201A1A" },
  ],
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      suppressHydrationWarning
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
        <Providers>
          <Navbar />
          <main className="flex-1">{children}</main>
          <Footer />
        </Providers>
      </body>
    </html>
  );
}
