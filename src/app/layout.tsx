import type { Metadata } from "next";
import { Geist } from "next/font/google";
import "./globals.css";

const geist = Geist({ subsets: ["latin"], variable: "--font-geist" });
export const metadata: Metadata = {
  title: "LifeVault — your life, in good order",
  description: "A calm, private home for the details that keep life moving.",
  applicationName: "LifeVault",
  manifest: "/manifest.webmanifest",
  appleWebApp: { capable: true, title: "LifeVault", statusBarStyle: "default" }
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en" suppressHydrationWarning><body className={geist.variable}>{children}</body></html>;
}