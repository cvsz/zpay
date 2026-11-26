/**
 * ZWallet Enterprise Backend Server
 * Provides portfolio, market price, and transaction APIs for the Android client.
 * Runs on Node.js with Express. All endpoints use HTTPS/WSS in production.
 */

const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3001;

// Security middleware
app.use(helmet());
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS || '*',
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization', 'X-Request-ID'],
}));
app.use(express.json({ limit: '10kb' }));

// Request ID middleware for traceability
app.use((req, res, next) => {
  req.requestId = req.headers['x-request-id'] || require('crypto').randomBytes(16).toString('hex');
  res.setHeader('X-Request-ID', req.requestId);
  next();
});

// --- MOCK PORTFOLIO DATA (matches PortfolioVisualizer.tsx) ---
const portfolioHistoryData = {
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
  ],
};

const blockchainAllocation = [
  { name: 'Bitcoin Network', value: 61500, percentage: 46.78, color: '#F7931A', nativeAsset: 'BTC' },
  { name: 'Ethereum Mainnet', value: 33400, percentage: 25.41, color: '#627EEA', nativeAsset: 'ETH' },
  { name: 'Solana Chain', value: 21450, percentage: 16.32, color: '#14F195', nativeAsset: 'SOL' },
  { name: 'Binance Smart Chain', value: 15100, percentage: 11.49, color: '#F3BA2F', nativeAsset: 'BNB' },
];

// --- API ROUTES ---

// Health check
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'zwallet-server',
    version: '1.0.0',
    timestamp: new Date().toISOString(),
    requestId: req.requestId,
  });
});

// Portfolio history
app.get('/api/portfolio/history', (req, res) => {
  const { timeframe = '1W' } = req.query;
  const data = portfolioHistoryData[timeframe] || portfolioHistoryData['1W'];
  res.json({
    timeframe,
    data,
    requestId: req.requestId,
  });
});

// Blockchain allocation
app.get('/api/portfolio/allocation', (req, res) => {
  res.json({
    allocation: blockchainAllocation,
    totalChains: blockchainAllocation.length,
    requestId: req.requestId,
  });
});

// Current portfolio total
app.get('/api/portfolio/summary', (req, res) => {
  const latest = portfolioHistoryData['1W'][portfolioHistoryData['1W'].length - 1];
  res.json({
    totalValue: latest.value,
    btcValue: latest.btcValue,
    ethValue: latest.ethValue,
    solValue: latest.solValue,
    currency: 'USD',
    requestId: req.requestId,
  });
});

// Market prices
app.get('/api/market/prices', (req, res) => {
  const prices = {
    BTC: { usd: 91520, change24h: 2.5 },
    ETH: { usd: 3340, change24h: -1.2 },
    SOL: { usd: 184.20, change24h: 5.2 },
    BNB: { usd: 582.60, change24h: 0.8 },
  };
  res.json({
    prices,
    timestamp: new Date().toISOString(),
    requestId: req.requestId,
  });
}

// Transaction history (mocked, idempotent-safe)
app.get('/api/transactions', (req, res) => {
  const transactions = [
    {
      id: 'tx-001',
      chain: 'Ethereum',
      type: 'Receive',
      fromAddress: '0x2e08demo21fa',
      toAddress: '0x71C7656EC7ab88b098defB751B7401B5f6d1476B',
      amountMinorUnit: 1500000000000000000,
      feeUsdMinorUnit: 1245,
      status: 'SUCCESS',
      timestamp: Date.now() - 86400000,
      notes: 'Inbound liquidity',
    },
    {
      id: 'tx-002',
      chain: 'Bitcoin',
      type: 'Send',
      fromAddress: 'bc1qxy2kg3f8px6pmre90ajvqd9676vazf8hxwgv4n',
      toAddress: 'bc1qxy2kg3f8px6pmre90ajvqd9676vazf8hxwgv4n',
      amountMinorUnit: 50000000,
      feeUsdMinorUnit: 850,
      status: 'SUCCESS',
      timestamp: Date.now() - 172800000,
      notes: 'P2WPKH transfer',
    },
  ];
  res.json({
    transactions,
    count: transactions.length,
    requestId: req.requestId,
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    error: 'Not Found',
    path: req.path,
    requestId: req.requestId,
  });
});

// Error handler
app.use((err, req, res, next) => {
  console.error(`[${req.requestId}] Server error:`, err.message);
  res.status(500).json({
    error: 'Internal Server Error',
    requestId: req.requestId,
  });
});

app.listen(PORT, () => {
  console.log(`ZWallet server running on port ${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
});

module.exports = app;
