package com.byagowi.persiancalendar.ui.calendar

// These aren't the most optimized implementations, probably going to be replaced with
// better one in the future though they should be enough for our use
class Graph(verticesCount: Int) {
    private val adjacency = List(verticesCount) { mutableSetOf<Int>() }

    override fun toString(): String = adjacency.toString()

    fun addEdge(x: Int, y: Int) {
        adjacency[x].add(y)
        adjacency[y].add(x)
    }

    // https://www.geeksforgeeks.org/connected-components-in-an-undirected-graph/
    fun connectedComponents() = sequence {
        val visited = BooleanArray(adjacency.size)
        adjacency.indices.forEach { if (!visited[it]) yield(sequence { dfs(it, visited) }) }
    }

    private suspend fun SequenceScope<Int>.dfs(v: Int, visited: BooleanArray) {
        visited[v] = true
        yield(v)
        adjacency[v].forEach { if (!visited[it]) dfs(it, visited) }
    }

    // https://www.geeksforgeeks.org/graph-coloring-set-2-greedy-algorithm/
    fun colors(): List<Int> {
        if (adjacency.isEmpty()) return emptyList()
        val result = MutableList(adjacency.size) { -1 }
        result[0] = 0
        val available = BooleanArray(adjacency.size)
        adjacency.indices.drop(1).forEach { x ->
            adjacency[x].forEach { if (result[it] != -1) available[result[it]] = true }
            result[x] = adjacency.indices.firstOrNull { !available[it] } ?: adjacency.size
            available.fill(false)
        }
        return result
    }
}