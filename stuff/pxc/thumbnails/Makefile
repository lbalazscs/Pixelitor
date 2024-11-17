CC = gcc
CFLAGS = -Wall -Wextra -pedantic -O2
LDFLAGS =

SRC = extract_pxc_thumbnails.c
TARGET = extract_pxc_thumbnails

all: $(TARGET)

$(TARGET): $(SRC)
	$(CC) $(CFLAGS) $(SRC) -o $(TARGET) $(LDFLAGS)

clean:
	rm -f $(TARGET)

.PHONY: all clean
