package com.example

import com.example.data.TransactionStateMachine
import com.example.data.TransactionStatus
import org.junit.Assert.*
import org.junit.Test

/** Regression tests for the transaction state machine (financial integrity invariant). */
class TransactionStateMachineTest {

    @Test
    fun `pending may transition to success`() {
        assertTrue(TransactionStateMachine.isValidTransition(TransactionStatus.PENDING, TransactionStatus.SUCCESS))
    }

    @Test
    fun `pending may transition to failed`() {
        assertTrue(TransactionStateMachine.isValidTransition(TransactionStatus.PENDING, TransactionStatus.FAILED))
    }

    @Test
    fun `pending may transition to reversed`() {
        assertTrue(TransactionStateMachine.isValidTransition(TransactionStatus.PENDING, TransactionStatus.REVERSED))
    }

    @Test
    fun `pending may not transition to pending`() {
        assertFalse(TransactionStateMachine.isValidTransition(TransactionStatus.PENDING, TransactionStatus.PENDING))
    }

    @Test
    fun `success is terminal and may not transition`() {
        assertFalse(TransactionStateMachine.isValidTransition(TransactionStatus.SUCCESS, TransactionStatus.FAILED))
        assertFalse(TransactionStateMachine.isValidTransition(TransactionStatus.SUCCESS, TransactionStatus.REVERSED))
        assertFalse(TransactionStateMachine.isValidTransition(TransactionStatus.SUCCESS, TransactionStatus.PENDING))
        assertTrue(TransactionStateMachine.isTerminal(TransactionStatus.SUCCESS))
    }

    @Test
    fun `failed is terminal and may not transition`() {
        assertFalse(TransactionStateMachine.isValidTransition(TransactionStatus.FAILED, TransactionStatus.SUCCESS))
        assertFalse(TransactionStateMachine.isValidTransition(TransactionStatus.FAILED, TransactionStatus.REVERSED))
        assertFalse(TransactionStateMachine.isValidTransition(TransactionStatus.FAILED, TransactionStatus.PENDING))
        assertTrue(TransactionStateMachine.isTerminal(TransactionStatus.FAILED))
    }

    @Test
    fun `reversed is terminal and may not transition`() {
        assertFalse(TransactionStateMachine.isValidTransition(TransactionStatus.REVERSED, TransactionStatus.SUCCESS))
        assertFalse(TransactionStateMachine.isValidTransition(TransactionStatus.REVERSED, TransactionStatus.FAILED))
        assertFalse(TransactionStateMachine.isValidTransition(TransactionStatus.REVERSED, TransactionStatus.PENDING))
        assertTrue(TransactionStateMachine.isTerminal(TransactionStatus.REVERSED))
    }

    @Test
    fun `unknown statuses are rejected`() {
        assertFalse(TransactionStateMachine.isValidTransition("UNKNOWN", TransactionStatus.SUCCESS))
        assertFalse(TransactionStateMachine.isValidTransition(TransactionStatus.PENDING, "UNKNOWN"))
        assertFalse(TransactionStateMachine.isTerminal("UNKNOWN"))
    }

    @Test
    fun `all statuses are terminal-known`() {
        assertEquals(4, TransactionStatus.all.size)
        assertTrue(TransactionStatus.all.contains(TransactionStatus.PENDING))
        assertTrue(TransactionStatus.all.contains(TransactionStatus.SUCCESS))
        assertTrue(TransactionStatus.all.contains(TransactionStatus.FAILED))
        assertTrue(TransactionStatus.all.contains(TransactionStatus.REVERSED))
    }
}
