class ResourceNotFoundException(Exception):
    """Equivalente ao ResourceNotFoundException do core Java — mapeado para HTTP 404."""

    def __init__(self, message: str) -> None:
        self.message = message
        super().__init__(message)


class BadRequestException(Exception):
    """Equivalente ao BadRequestException do core Java — mapeado para HTTP 400."""

    def __init__(self, message: str) -> None:
        self.message = message
        super().__init__(message)
