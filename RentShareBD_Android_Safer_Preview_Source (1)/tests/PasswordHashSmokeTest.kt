package com.example.data.local
fun main() {
    val strong = "aDifferentTestPassword48!"
    val h1 = LocalPasswordHasher.hash(strong)
    val h2 = LocalPasswordHasher.hash(strong)
    check(h1 != h2) { "salt reuse detected" }
    check(LocalPasswordHasher.verify(strong,h1)) { "valid password rejected" }
    check(!LocalPasswordHasher.verify("wrong",h1)) { "invalid password accepted" }
    check(!LocalPasswordHasher.verify(strong, "pbkdf2-sha256:9:not-valid:not-valid"))
    check(LocalPasswordHasher.verify("old-legacy-demo", "old-legacy-demo"))
    println("5 local password hashing checks passed")
}
