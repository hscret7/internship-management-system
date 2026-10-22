import os
import re

dirs = [r'c:\Users\MY_PC\source\internship-management-system\src\main\webapp\hr',
        r'c:\Users\MY_PC\source\internship-management-system\src\main\webapp\intern']
count = 0

for d in dirs:
    for root, _, files in os.walk(d):
        for file in files:
            if file.endswith('.jsp'):
                path = os.path.join(root, file)
                with open(path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Check if csrf token is already present
                if 'name="csrfToken"' in content:
                    continue
                
                # Regex to match <form ... method="post" ... >
                pattern = re.compile(r'(<form[^>]*method=[\"\']post[\"\'][^>]*>)', re.IGNORECASE)
                
                # Insert the hidden input right after the form opening tag
                replacement = r'\1\n    <input type="hidden" name="csrfToken" value="${csrfToken}" />'
                new_content = pattern.sub(replacement, content)
                
                if new_content != content:
                    with open(path, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f'Updated {path}')
                    count += 1

print(f'Done. Updated {count} files.')

