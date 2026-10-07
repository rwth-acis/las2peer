#!/usr/bin/env bash
# Local Ethereum dev chain for the las2peer registry (ganache 7, in-memory).
# The keys are the ones las2peer derives from the 10 well-known dev mnemonics
# (see las2peer-ethereum-cluster start.sh) — las2peer derives keys from the raw
# BIP39 seed, not the BIP44 path ganache uses, so a plain --mnemonic won't fund them.
set -euo pipefail
BALANCE=0xD3C21BCECCEDA1000000 # 1,000,000 ETH
KEYS=(
  0x95395d5cd006cf4a95a01f219a66205d4b7d02e54b0da326e9154fa618f083fa
  0xaae2ed23109633f821c9bd39cfd2d7de2ff6dd49796663a30b3eb5b6880c63db
  0xe5c65369fb1021efbc6f1317da46407ba87bf510611457e25dde4eca7c8e80d3
  0x3ad62eea7750b111613227e78620054b85f290f93b8fd6284fef44d974fc5f91
  0xbc2f0a9bc4628f12aa5a8fc697b2f43ffed83da0496b881fa81d45a86909b1df
  0x3bbd32652e6e86c6f7c6e4e3bdd950afaa83d274c097c28a509dcd9569051787
  0xbe1c179fbfd7ad41bcee685d56d13bcc6dd555ac2bb5a97e095adc75b9397441
  0x8318fa509f77c65fb233d2f143dc7276fe2cf3d8538ad15ed31b14e87b39222c
  0xf14be57edb9e8ebe272e0f4875ae3737217273657307116a459ccc30abae25b1
  0xea9350dc7c1a7cff22832576d555590ea32f83e3fcccd2e55b74475075117481
)
ARGS=()
for k in "${KEYS[@]}"; do ARGS+=(--wallet.accounts "$k,$BALANCE"); done
# istanbul: pre-EIP-1559, so truffle 5.0 and web3j 4.5 send legacy transactions without issues
exec npx -y ganache@7.9.2 --chain.networkId 456719 --chain.chainId 456719 --chain.hardfork istanbul \
  --miner.blockGasLimit 6721975 --miner.coinbase 0xb5a66d27457af8be2a09f17add73c2ae46520e69 --logging.quiet "${ARGS[@]}"
