#!/usr/bin/env python3
"""
Synchronize translation properties files with a master file.

This script ensures that all translation files maintain the same structure
(keys, comments, empty lines) as the master file, while preserving existing
translations. If a translation is missing, the English value is used.

Usage:
    python sync_translations.py master.properties translation1.properties [translation2.properties ...]
"""

import sys
import os
import re


def parse_properties_file(file_path):
    """
    Parse a properties file into a list of lines with their types and values.
    
    Returns:
        A list of tuples: (line_type, key, value, original_line)
        Where line_type is one of: 'comment', 'empty', 'property'
    """
    result = []
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.rstrip('\n')
                
                # Empty line
                if not line.strip():
                    result.append(('empty', None, None, line))
                    continue
                
                # Comment line
                if line.strip().startswith('#'):
                    result.append(('comment', None, None, line))
                    continue
                
                # Property line
                match = re.match(r'^([^=]+)=(.*)$', line)
                if match:
                    key = match.group(1).strip()
                    value = match.group(2)
                    result.append(('property', key, value, line))
                else:
                    # Malformed line - treat as comment for safety
                    result.append(('comment', None, None, line))
        
        return result
    except Exception as e:
        print(f"Error reading file {file_path}: {e}", file=sys.stderr)
        sys.exit(1)


def build_translation_dict(parsed_file):
    """
    Create a dictionary of key-value pairs from the parsed properties file.
    
    Returns:
        A dictionary mapping keys to values
    """
    translations = {}
    for line_type, key, value, _ in parsed_file:
        if line_type == 'property' and key is not None:
            translations[key] = value
    return translations


def sync_translation_file(master_file_data, translation_file_path):
    """
    Synchronize a translation file with the master file.
    
    Args:
        master_file_data: Parsed data from the master file
        translation_file_path: Path to the translation file to update
    """
    # Parse translation file
    translation_file_data = parse_properties_file(translation_file_path)
    
    # Build translation dictionary from existing translations
    translations = build_translation_dict(translation_file_data)
    
    # Create new translation file content based on master file structure
    new_content = []
    needs_update = False
    
    for line_type, key, value, original_line in master_file_data:
        if line_type == 'empty' or line_type == 'comment':
            # Copy empty lines and comments directly
            new_content.append(original_line)
        elif line_type == 'property':
            # Use existing translation if available, otherwise use master value
            if key in translations:
                translated_value = translations[key]
                new_line = f"{key}={translated_value}"
            else:
                # Translation not found, use master value
                new_line = f"{key}={value}"
                needs_update = True
                print(f"Warning: Missing translation for '{key}' in {translation_file_path}")
            new_content.append(new_line)
    
    # Check if the content is different from the original translation file
    original_content = [line for _, _, _, line in translation_file_data]
    if new_content != original_content:
        needs_update = True
    
    # Write updated content if needed
    if needs_update:
        print(f"Updating {translation_file_path}")
        try:
            with open(translation_file_path, 'w', encoding='utf-8') as f:
                for line in new_content:
                    f.write(line + '\n')
        except Exception as e:
            print(f"Error writing to {translation_file_path}: {e}", file=sys.stderr)
            sys.exit(1)
    else:
        print(f"No changes needed for {translation_file_path}")


def main():
    print("Starting...")
    if len(sys.argv) < 3:
        print("Usage: python sync_translations.py master.properties translation1.properties [translation2.properties ...]")
        sys.exit(1)
    
    master_file = sys.argv[1]
    translation_files = sys.argv[2:]
    
    # Check if files exist
    if not os.path.isfile(master_file):
        print(f"Error: Master file '{master_file}' not found.", file=sys.stderr)
        sys.exit(1)
    
    for file in translation_files:
        if not os.path.isfile(file):
            print(f"Error: Translation file '{file}' not found.", file=sys.stderr)
            sys.exit(1)
    
    # Parse master file
    master_file_data = parse_properties_file(master_file)
    
    # Process each translation file
    for translation_file in translation_files:
        sync_translation_file(master_file_data, translation_file)
    
    print("Synchronization complete.")


if __name__ == "__main__":
    main()