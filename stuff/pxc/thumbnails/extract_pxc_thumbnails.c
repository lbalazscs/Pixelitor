#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

// reads a 32-bit big-endian integer from a file
uint32_t read_uint32(FILE *file) {
    uint32_t value = 0;
    uint8_t bytes[4];
    
    if (fread(bytes, 1, 4, file) != 4) {
        return 0;
    }
    
    value = (bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3];
    return value;
}

int main(int argc, char *argv[]) {
    FILE *input_file;
    FILE *output_file;
    uint8_t identification_bytes[2];
    uint8_t version;
    uint32_t thumbnail_size;
    uint8_t *thumbnail_data;
    
    if (argc != 3) {
        fprintf(stderr, "Usage: %s <input.pxc> <output.png>\n", argv[0]);
        return 1;
    }
    
    input_file = fopen(argv[1], "rb");
    if (!input_file) {
        fprintf(stderr, "Error: Could not open input file '%s'\n", argv[1]);
        return 1;
    }
    
    // check PXC format
    if (fread(identification_bytes, 1, 2, input_file) != 2) {
        fprintf(stderr, "Error: Could not read identification bytes\n");
        fclose(input_file);
        return 1;
    }
    if (identification_bytes[0] != 0xAB || identification_bytes[1] != 0xC4) {
        fprintf(stderr, "Error: Invalid PXC file format\n");
        fclose(input_file);
        return 1;
    }
    
    // check version
    if (fread(&version, 1, 1, input_file) != 1) {
        fprintf(stderr, "Error: Could not read version byte\n");
        fclose(input_file);
        return 1;
    }
    if (version < 0x04) {
        fprintf(stderr, "Error: This PXC file (version %d) does not contain a thumbnail\n", version);
        fclose(input_file);
        return 1;
    }
    
    // read thumbnail 
    thumbnail_size = read_uint32(input_file);
    if (thumbnail_size == 0 || thumbnail_size > 1024 * 1024) { // Max 1MB for safety
        fprintf(stderr, "Error: Invalid thumbnail size\n");
        fclose(input_file);
        return 1;
    }
    thumbnail_data = (uint8_t *)malloc(thumbnail_size);
    if (!thumbnail_data) {
        fprintf(stderr, "Error: Could not allocate memory for thumbnail\n");
        fclose(input_file);
        return 1;
    }
    if (fread(thumbnail_data, 1, thumbnail_size, input_file) != thumbnail_size) {
        fprintf(stderr, "Error: Could not read thumbnail data\n");
        free(thumbnail_data);
        fclose(input_file);
        return 1;
    }
    
    // write output file
    output_file = fopen(argv[2], "wb");
    if (!output_file) {
        fprintf(stderr, "Error: Could not create output file '%s'\n", argv[2]);
        free(thumbnail_data);
        fclose(input_file);
        return 1;
    }
    if (fwrite(thumbnail_data, 1, thumbnail_size, output_file) != thumbnail_size) {
        fprintf(stderr, "Error: Could not write thumbnail data\n");
        free(thumbnail_data);
        fclose(input_file);
        fclose(output_file);
        return 1;
    }
    
    free(thumbnail_data);
    fclose(input_file);
    fclose(output_file);
    
    printf("Successfully extracted thumbnail to '%s'\n", argv[2]);
    return 0;
}
