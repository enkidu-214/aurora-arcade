package com.aurora.arcade.core

data class Cell(val x: Int, val y: Int)
object Pieces {
    private val shapes = mapOf(
        Kind.I to listOf(Cell(0,1),Cell(1,1),Cell(2,1),Cell(3,1)),
        Kind.O to listOf(Cell(1,0),Cell(2,0),Cell(1,1),Cell(2,1)),
        Kind.T to listOf(Cell(1,0),Cell(0,1),Cell(1,1),Cell(2,1)),
        Kind.S to listOf(Cell(1,0),Cell(2,0),Cell(0,1),Cell(1,1)),
        Kind.Z to listOf(Cell(0,0),Cell(1,0),Cell(1,1),Cell(2,1)),
        Kind.J to listOf(Cell(0,0),Cell(0,1),Cell(1,1),Cell(2,1)),
        Kind.L to listOf(Cell(2,0),Cell(0,1),Cell(1,1),Cell(2,1)),
    )
    private val rotations = Kind.entries.associateWith { kind ->
        val turns = mutableListOf(shapes.getValue(kind))
        repeat(3) { turns += if (kind == Kind.O) turns.last() else turns.last().map { Cell((if (kind == Kind.I) 3 else 2) - it.y, it.x) } }
        turns.toList()
    }
    fun cells(kind: Kind, rotation: Int = 0): List<Cell> = rotations.getValue(kind)[rotation]
    fun cells(piece: Piece) = cells(piece.kind, piece.rotation).map { Cell(it.x + piece.x, it.y + piece.y) }
    // Official SRS convention uses Cartesian y; screen y points down.
    fun kicks(kind: Kind, from: Int, to: Int): List<Cell> {
        val offsets = if (kind == Kind.I) when (from to to) {
            0 to 1 -> listOf(0 to 0,-2 to 0,1 to 0,-2 to -1,1 to 2)
            1 to 0 -> listOf(0 to 0,2 to 0,-1 to 0,2 to 1,-1 to -2)
            1 to 2 -> listOf(0 to 0,-1 to 0,2 to 0,-1 to 2,2 to -1)
            2 to 1 -> listOf(0 to 0,1 to 0,-2 to 0,1 to -2,-2 to 1)
            2 to 3 -> listOf(0 to 0,2 to 0,-1 to 0,2 to 1,-1 to -2)
            3 to 2 -> listOf(0 to 0,-2 to 0,1 to 0,-2 to -1,1 to 2)
            3 to 0 -> listOf(0 to 0,1 to 0,-2 to 0,1 to -2,-2 to 1)
            else -> listOf(0 to 0,-1 to 0,2 to 0,-1 to 2,2 to -1)
        } else when (from to to) {
            0 to 1, 2 to 1 -> listOf(0 to 0,-1 to 0,-1 to 1,0 to -2,-1 to -2)
            1 to 0, 1 to 2 -> listOf(0 to 0,1 to 0,1 to -1,0 to 2,1 to 2)
            2 to 3, 0 to 3 -> listOf(0 to 0,1 to 0,1 to 1,0 to -2,1 to -2)
            else -> listOf(0 to 0,-1 to 0,-1 to -1,0 to 2,-1 to 2)
        }
        return offsets.map { Cell(it.first, -it.second) }
    }
}
