package com.semstress.mobile.domain

/** GP-05: sentinel stage ids for sessions that aren't part of the normal stage catalog. */
const val DAILY_CHALLENGE_STAGE_ID = -1
const val ZEN_MODE_STAGE_ID = -2

const val DAILY_CHALLENGE_MAX_ATTEMPTS = 3

/** GP-05: the daily board is the same for everyone - the seed is simply today's epoch day. */
fun dailyChallengeSeed(epochDay: Long): Long = epochDay
