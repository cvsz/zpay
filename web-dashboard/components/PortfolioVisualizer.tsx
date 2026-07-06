import React, { useState } from 'react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';
import { 
  TrendingUp, 
  TrendingDown, 
  Activity, 
  Layers, 
  Wallet, 
  ArrowUpRight, 
  Calendar,
  Lock,
  Globe
} from 'lucide-react';

// --- TYPE DEFINITIONS ---
interface PortfolioHistoryItem {
  timestamp: string;
  value: number;
  btcValue: number;
  ethValue: number;
  solValue: number;
}

interface AllocationItem {
  name: string;
  value: number;
  percentage: number;
  color: string;
  nativeAsset: string;
}

// --- MOCK DATA MATCHING ZWALLET STATE ---
const portfolioHistoryData: Record<string, PortfolioHistoryItem[]> = {
  '1W': [
    { timestamp: 'Mon', value: 120500, btcValue: 55000, ethValue: 45000, solValue: 20500 },
    { timestamp: 'Tue', value: 122100, btcValue: 56200, ethValue: 45400, solValue: 20500 },
    { timestamp: 'Wed', value: 119800, btcValue: 54100, ethValue: 44900, solValue: 20800 },
    { timestamp: 'Thu', value: 124500, btcValue: 58000, ethValue: 45500, solValue: 21000 },
    { timestamp: 'Fri', value: 127200, btcValue: 59100, ethValue: 46800, solValue: 21300 },
    { timestamp: 'Sat', value: 126900, btcValue: 58800, ethValue: 46900, solValue: 21200 },
    { timestamp: 'Sun', value: 131450, btcValue: 61500, ethValue: 48500, solValue: 21450 },
  ],
  '1M': [
    { timestamp: 'Week 1', value: 112000, btcValue: 50000, ethValue: 43000, solValue: 19000 },
    { timestamp: 'Week 2', value: 118500, btcValue: 54000, ethValue: 44500, solValue: 20000 },
    { timestamp: 'Week 3', value: 124000, btcValue: 57500, ethValue: 46000, solValue: 20500 },
    { timestamp: 'Week 4', value: 131450, btcValue: 61500, ethValue: 48500, solValue: 21450 },
  ],
  '1Y': [
    { timestamp: 'Q1', value: 85000, btcValue: 40000, ethValue: 32000, solValue: 13000 },
    { timestamp: 'Q2', value: 98000, btcValue: 45000, ethValue: 37000, solValue: 16000 },
    { timestamp: 'Q3', value: 115000, btcValue: 52000, ethValue: 43000, solValue: 20000 },
    { timestamp: 'Q4', value: 131450, btcValue: 61500, ethValue: 48500, solValue: 21450 },
  ]
};

const blockchainAllocation: AllocationItem[] = [
  { name: 'Bitcoin Network', value: 61500, percentage: 46.78, color: '#F7931A', nativeAsset: 'BTC' },
  { name: 'Ethereum Mainnet', value: 33400, percentage: 25.41, color: '#627EEA', nativeAsset: 'ETH' },
  { name: 'Solana Chain', value: 21450, percentage: 16.32, color: '#14F195', nativeAsset: 'SOL' },
  { name: 'Binance Smart Chain', value: 15100, percentage: 11.49, color: '#F3BA2F', nativeAsset: 'BNB' },
];

export default function PortfolioVisualizer() {
  const [timeframe, setTimeframe] = useState<'1W' | '1M' | '1Y'>('1W');
  const [hoveredData, setHoveredData] = useState<PortfolioHistoryItem | null>(null);

  const currentData = portfolioHistoryData[timeframe];
  const activeValue = hoveredData ? hoveredData.value : currentData[currentData.length - 1].value;
  const initialValue = currentData[0].value;
  const changeUsd = activeValue - initialValue;
  const changePercent = (changeUsd / initialValue) * 100;
  const isPositive = changeUsd >= 0;

  return (
    <div className="w-full min-h-screen bg-[#0b0c10] text-[#c5c6c7] p-6 font-sans">
      {/* HEADER SECTION */}
      <div className="max-w-7xl mx-auto mb-8">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between border-b border-[#1f2833] pb-6">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="w-2 h-2 rounded-full bg-[#14F195] animate-pulse"></span>
              <p className="text-xs font-bold uppercase tracking-wider text-[#66fcf1]">
                ZWallet Enterprise Node Active
              </p>
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight text-white flex items-center gap-3">
              <Wallet className="text-[#66fcf1] w-8 h-8" />
              Multi-Chain Portfolio Portal
            </h1>
          </div>

          {/* Timeframe Filter Buttons */}
          <div className="flex items-center gap-2 mt-4 md:mt-0 bg-[#1f2833] p-1.5 rounded-lg border border-[#c5c6c7]/10">
            {(['1W', '1M', '1Y'] as const).map((t) => (
              <button
                key={t}
                onClick={() => setTimeframe(t)}
                className={`px-4 py-1.5 rounded-md text-xs font-bold transition-all duration-300 ${
                  timeframe === t
                    ? 'bg-[#66fcf1] text-[#0b0c10] shadow-[0_0_12px_rgba(102,252,241,0.3)]'
                    : 'hover:text-white text-[#c5c6c7]/60'
                }`}
              >
                {t === '1W' ? '7 DAYS' : t === '1M' ? '30 DAYS' : '1 YEAR'}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* LEFT & CENTER PANELS: PORTFOLIO VALUE OVER TIME */}
        <div className="lg:col-span-2 space-y-6">
          
          {/* BALANCE METRICS CARD */}
          <div className="bg-[#1f2833] rounded-2xl border border-[#c5c6c7]/10 p-6 shadow-xl relative overflow-hidden">
            <div className="absolute top-0 right-0 p-8 opacity-5">
              <Activity className="w-48 h-48 text-[#66fcf1]" />
            </div>

            <div className="relative z-10 flex flex-col md:flex-row md:items-end justify-between gap-4">
              <div>
                <p className="text-xs font-bold text-[#66fcf1]/80 uppercase tracking-widest mb-1">
                  Secure Aggregated Balance
                </p>
                <div className="text-4xl md:text-5xl font-mono font-black text-white tracking-tight">
                  ${activeValue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  <span className="text-lg font-bold text-[#c5c6c7]/50 ml-2">USD</span>
                </div>
              </div>

              <div className="flex flex-col items-start md:items-end">
                <p className="text-xs text-[#c5c6c7]/50 mb-1 font-semibold flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5" /> Period Change ({timeframe})
                </p>
                <div
                  className={`flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold ${
                    isPositive 
                      ? 'bg-[#14F195]/15 text-[#14F195]' 
                      : 'bg-[#ff007f]/15 text-[#ff007f]'
                  }`}
                >
                  {isPositive ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
                  <span>
                    {isPositive ? '+' : ''}
                    {changeUsd.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} (
                    {changePercent.toFixed(2)}%)
                  </span>
                </div>
              </div>
            </div>

            {/* AREA CHART */}
            <div className="h-[340px] mt-8 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart
                  data={currentData}
                  onMouseMove={(e) => {
                    if (e && e.activePayload && e.activePayload[0]) {
                      setHoveredData(e.activePayload[0].payload as PortfolioHistoryItem);
                    }
                  }}
                  onMouseLeave={() => setHoveredData(null)}
                  margin={{ top: 10, right: 5, left: -20, bottom: 0 }}
                >
                  <defs>
                    <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#66fcf1" stopOpacity={0.4} />
                      <stop offset="95%" stopColor="#66fcf1" stopOpacity={0.0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#c5c6c7" strokeOpacity={0.05} vertical={false} />
                  <XAxis
                    dataKey="timestamp"
                    stroke="#c5c6c7"
                    strokeOpacity={0.3}
                    tickLine={false}
                    axisLine={false}
                    tick={{ fontSize: 11, fontWeight: 'semibold' }}
                  />
                  <YAxis
                    stroke="#c5c6c7"
                    strokeOpacity={0.3}
                    tickLine={false}
                    axisLine={false}
                    tick={{ fontSize: 11, fontWeight: 'semibold' }}
                    domain={['dataMin - 5000', 'dataMax + 5000']}
                    tickFormatter={(value) => `$${value / 1000}k`}
                  />
                  <Tooltip
                    cursor={{ stroke: '#66fcf1', strokeWidth: 1, strokeDasharray: '4 4' }}
                    content={({ active, payload }) => {
                      if (active && payload && payload.length) {
                        const data = payload[0].payload as PortfolioHistoryItem;
                        return (
                          <div className="bg-[#1f2833] border border-[#66fcf1]/30 p-4 rounded-xl shadow-2xl backdrop-blur-md">
                            <p className="text-xs text-[#c5c6c7]/60 font-bold mb-2">{data.timestamp}</p>
                            <div className="space-y-1">
                              <p className="text-sm font-bold text-white flex justify-between gap-6">
                                <span>Portfolio:</span>
                                <span className="font-mono text-[#66fcf1]">${data.value.toLocaleString()}</span>
                              </p>
                              <div className="text-[10px] text-[#c5c6c7]/50 space-y-0.5 border-t border-[#c5c6c7]/10 pt-1.5 mt-1.5 font-mono">
                                <p className="flex justify-between">
                                  <span>BTC Alloc:</span>
                                  <span>${data.btcValue.toLocaleString()}</span>
                                </p>
                                <p className="flex justify-between">
                                  <span>ETH Alloc:</span>
                                  <span>${data.ethValue.toLocaleString()}</span>
                                </p>
                                <p className="flex justify-between">
                                  <span>SOL Alloc:</span>
                                  <span>${data.solValue.toLocaleString()}</span>
                                </p>
                              </div>
                            </div>
                          </div>
                        );
                      }
                      return null;
                    }}
                  />
                  <Area
                    type="monotone"
                    dataKey="value"
                    stroke="#66fcf1"
                    strokeWidth={3}
                    fillOpacity={1}
                    fill="url(#colorValue)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* BLOCKCHAIN METRICS SUMMARY */}
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
            {blockchainAllocation.map((chain) => (
              <div 
                key={chain.name}
                className="bg-[#1f2833] border border-[#c5c6c7]/10 rounded-xl p-4 flex flex-col justify-between hover:border-[#66fcf1]/30 transition-all duration-300 group"
              >
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span 
                      className="text-[10px] font-black px-2.5 py-0.5 rounded-full uppercase"
                      style={{ backgroundColor: `${chain.color}15`, color: chain.color }}
                    >
                      {chain.nativeAsset}
                    </span>
                    <span className="text-xs font-bold text-white group-hover:text-[#66fcf1] transition-colors">
                      {chain.percentage}%
                    </span>
                  </div>
                  <p className="text-[10px] text-[#c5c6c7]/50 font-semibold uppercase tracking-wider truncate">
                    {chain.name}
                  </p>
                </div>
                <div className="mt-3 flex items-baseline justify-between">
                  <span className="text-sm font-mono font-black text-white">
                    ${chain.value.toLocaleString()}
                  </span>
                  <ArrowUpRight className="w-3.5 h-3.5 text-[#c5c6c7]/30 group-hover:text-[#66fcf1] transition-transform duration-300 transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
                </div>
              </div>
            ))}
          </div>

        </div>

        {/* RIGHT PANEL: ASSET ALLOCATION across BLOCKCHAINS */}
        <div className="space-y-6">
          
          <div className="bg-[#1f2833] rounded-2xl border border-[#c5c6c7]/10 p-6 shadow-xl flex flex-col h-full justify-between">
            <div>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-md font-bold tracking-tight text-white flex items-center gap-2">
                  <Layers className="text-[#66fcf1] w-5 h-5" />
                  Multi-Chain Allocation
                </h3>
                <span className="text-[10px] text-[#66fcf1] bg-[#66fcf1]/10 px-2 py-0.5 rounded-full font-bold">
                  SECURE LIVE FEED
                </span>
              </div>
              <p className="text-xs text-[#c5c6c7]/60 mb-6 leading-relaxed">
                Aggregated cryptographic balances split across isolated HSM-monitored blockchain architectures.
              </p>

              {/* PIE CHART CONTAINER */}
              <div className="h-[220px] flex items-center justify-center relative">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={blockchainAllocation}
                      cx="50%"
                      cy="50%"
                      innerRadius={65}
                      outerRadius={85}
                      paddingAngle={5}
                      dataKey="value"
                    >
                      {blockchainAllocation.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip
                      content={({ active, payload }) => {
                        if (active && payload && payload.length) {
                          const data = payload[0].payload as AllocationItem;
                          return (
                            <div className="bg-[#0b0c10] border border-[#c5c6c7]/20 px-3 py-2 rounded-lg text-xs">
                              <span className="font-bold text-white block mb-1">{data.name}</span>
                              <span className="font-mono text-[#66fcf1]">${data.value.toLocaleString()} ({data.percentage}%)</span>
                            </div>
                          );
                        }
                        return null;
                      }}
                    />
                  </PieChart>
                </ResponsiveContainer>

                {/* Donut Center Label */}
                <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
                  <span className="text-[10px] uppercase font-bold text-[#c5c6c7]/40 tracking-widest">
                    TOTAL CHAINS
                  </span>
                  <span className="text-2xl font-mono font-black text-white">
                    {blockchainAllocation.length}
                  </span>
                </div>
              </div>
            </div>

            {/* ALLOCATION LIST */}
            <div className="space-y-3.5 mt-6 border-t border-[#c5c6c7]/10 pt-6">
              {blockchainAllocation.map((item) => (
                <div key={item.name} className="flex items-center justify-between group">
                  <div className="flex items-center gap-3">
                    <span 
                      className="w-2.5 h-2.5 rounded-full shadow-inner"
                      style={{ backgroundColor: item.color }}
                    ></span>
                    <div>
                      <p className="text-xs font-bold text-white group-hover:text-[#66fcf1] transition-colors">
                        {item.name}
                      </p>
                      <p className="text-[10px] text-[#c5c6c7]/50 font-medium">
                        {item.nativeAsset} Network Gateway
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-xs font-bold font-mono text-white">
                      ${item.value.toLocaleString()}
                    </p>
                    <p className="text-[10px] font-medium text-[#c5c6c7]/40">
                      {item.percentage}%
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* COMPLIANCE & SECURITY CARD */}
          <div className="bg-[#1f2833]/55 border border-[#c5c6c7]/10 rounded-2xl p-5 shadow-lg">
            <div className="flex gap-4">
              <div className="p-2.5 bg-[#66fcf1]/10 rounded-xl h-fit">
                <Lock className="w-5 h-5 text-[#66fcf1]" />
              </div>
              <div>
                <h4 className="text-xs font-bold text-white mb-1 tracking-wide uppercase">
                  PCI-DSS SECURE VAULT ENVIRONMENT
                </h4>
                <p className="text-[11px] text-[#c5c6c7]/60 leading-relaxed">
                  Real-time synchronization uses hardware-backed enclave keys (Android Keystore / Apple Secure Enclave). All transaction state channels are encrypted locally before reporting to dashboard indexers.
                </p>
              </div>
            </div>
          </div>

        </div>

      </div>
    </div>
  );
}
