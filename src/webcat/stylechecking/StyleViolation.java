package webcat.stylechecking;

public record StyleViolation(StyleViolationSource source, String filePath, int beginLine, int endLine, String message) {

}

