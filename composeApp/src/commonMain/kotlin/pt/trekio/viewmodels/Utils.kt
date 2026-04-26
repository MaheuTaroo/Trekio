package pt.trekio.viewmodels

fun String.isValidName(): Pair<Boolean, String> {
    val letters = 'a'..'z' union 'A'..'Z'
    val possibleChars = (letters union '0'..'9') + '_' + '.'
    if (this.length !in 3..32) return false to "Username must be between 3 and 32 characters long"
    if (this.any { l -> l !in possibleChars }) {
        return false to
            "Username can only have uppercase and lowercase letters, digits, periods and underscores"
    }
    if (this[0] !in letters) return false to "Username must start with a letter"
    return true to ""
}

fun String.isValidEmail(): Pair<Boolean, String> {
    val alphaNumeric = ('a'..'z' union 'A'..'Z' union '0'..'9')
    val consecutiveSpecialChars = Regex("[^a-zA-Z0-9]{2,}")
    if (this.any { l -> l.isWhitespace() }) return false to "Email should not contain whitespaces"
    if (consecutiveSpecialChars in this) return false to "Email should not contain 2 consecutive non-alphanumeric characters"
    val parts = this.split("@")
    if (parts.size != 2) return false to "Email should contain one and only one '@'"
    val (local, domain) = parts
    if (local.isEmpty() || local[0] !in alphaNumeric) return false to "Email should start with a uppercase and lowercase letters or a digit"
    if (domain.isEmpty() || domain.last() !in alphaNumeric) return false to "Email should not end with a special character"
    val domainParts = domain.split('.')
    if (domainParts.size < 2) return false to "Email should contain at least one period for top-level domain"
    return true to ""
}

fun String.isSafePassword(): Pair<Boolean, String> {
    val lowercase = 'a'..'z'
    val uppercase = 'A'..'Z'
    val digits = '0'..'9'
    val symbols = """!"#$%&/()=?»@£§€{[]}«+*-<>\|;:_"""
    if (this.any { l -> l.isWhitespace() }) return false to "Password should not contain whitespaces"
    if (this.length <= 7) return false to "Password must be at least 8 characters long"
    if (this.none { it in lowercase }) return false to "Password must contain at least one lowercase letter"
    if (this.none { it in uppercase }) return false to "Password must contain at least one uppercase letter"
    if (this.none { it in digits }) return false to "Password must contain at least one digit"
    if (this.none { it in symbols }) return false to "Password must contain at least one symbol"
    return true to ""
}
