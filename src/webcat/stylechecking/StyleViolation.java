package webcat.stylechecking;

public record StyleViolation(String filePath, int beginLine, int endLine, String message) {

}

