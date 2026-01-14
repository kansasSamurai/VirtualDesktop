def number = 0
new File("C:\\dev\\workspaces\\textfiles\\test.html").eachLine { line ->
    number++
    println "$number: $line"
}
