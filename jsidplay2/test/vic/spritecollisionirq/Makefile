NAME = spritecollisionirq
BINARY = $(NAME).prg
SOURCE = $(NAME).asm
COMPILER = acme
CFLAGS = -v1 --cpu 6502 -f cbm -o
DEBUGGER = x64
DEBUGFLAGS = +sound
DTVTRANS = dtv2sertrans
REMOVE = rm -f

.PHONY: all test debug upload clean

all: $(BINARY)

test: $(BINARY)
	$(DTVTRANS) reset + load $(BINARY) + run

upload: $(BINARY)
	$(DTVTRANS) reset + wr $(BINARY)

debug: $(BINARY)
	$(DEBUGGER) $(DEBUGFLAGS) $(BINARY)

$(BINARY): $(SOURCE)
	$(COMPILER) $(CFLAGS) $@ $<

clean:
	-$(REMOVE) $(BINARY)
