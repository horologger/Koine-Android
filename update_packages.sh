#!/bin/bash\nfor file in $(find app/src -name "*.kt"); do\n  sed -i "" "s/package com\.geeksville\.mesh/package com.koine.mesh/g" "$file"\ndone
