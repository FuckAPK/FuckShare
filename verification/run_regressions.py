#!/usr/bin/env python3
"""Run JVM regressions against production Kotlin sources without Gradle or an Android SDK."""
from pathlib import Path
import os
import subprocess
import tempfile
import sys

ROOT = Path(__file__).resolve().parent.parent
CACHE = Path(os.environ.get('GRADLE_USER_HOME', Path.home() / '.gradle')) / 'caches/modules-2/files-2.1'
VERSION = '2.2.20'

def jar(group, artifact, version):
    matches = list((CACHE / group / artifact / version).rglob('*.jar'))
    if not matches:
        raise SystemExit(f'Missing cached dependency: {group}:{artifact}:{version} in {CACHE}')
    return matches[0]

compiler = [jar('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', VERSION),
            jar('org.jetbrains.kotlin', 'kotlin-stdlib', VERSION),
            jar('org.jetbrains.kotlin', 'kotlin-script-runtime', VERSION),
            jar('org.jetbrains.kotlin', 'kotlin-reflect', '2.2.0'),
            jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.8.0'),
            jar('org.jetbrains', 'annotations', '13.0')]

def source(relative):
    return (ROOT / relative).read_text()

def method(text, declaration):
    start = text.index(declaration)
    # Production methods end at the next top-level member or documentation block.
    end = text.find('\n    }', start) + len('\n    }')
    assert end > start, declaration
    return text[start:end]

share = 'app/src/main/kotlin/org/lyaaz/fuckshare/'

with tempfile.TemporaryDirectory(prefix='fuckapk-regressions-') as temp:
    work = Path(temp)
    def run(name, originals, generated):
        folder = work / name
        folder.mkdir()
        paths = [ROOT / p for p in originals]
        for filename, text in generated.items():
            path = folder / filename
            path.write_text(text)
            paths.append(path)
        args = ['-no-stdlib', '-no-reflect', '-jvm-target', '21', '-classpath', str(compiler[1]),
                '-d', str(folder / 'classes'), *map(str, paths)]
        command = ['java', '-cp', os.pathsep.join(map(str, compiler)),
                   'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', *args]
        compile_result = subprocess.run(command, capture_output=True, text=True)
        if compile_result.returncode:
            print(compile_result.stdout + compile_result.stderr, file=sys.stderr)
            raise SystemExit(compile_result.returncode)
        subprocess.run(['java', '-Djava.awt.headless=true', '-cp',
                        os.pathsep.join([str(folder / 'classes'), str(compiler[1])]),
                        'RegressionKt'], check=True)
        print(f'{name}: PASS', flush=True)

    logging = 'package timber.log\nobject Timber { fun d(vararg a: Any?) {}; fun e(vararg a: Any?) {}; fun w(vararg a: Any?) {} }'
    fileutils = source(share + 'utils/FileUtils.kt')
    run('parsers', [share + p for p in ['utils/ByteUtils.kt', 'exifhelper/ImageFormatException.kt',
        'exifhelper/JpegExifHelper.kt', 'exifhelper/PngExifHelper.kt', 'exifhelper/WebpExifHelper.kt']], {
        'Logging.kt': logging,
        'ExifHelper.kt': 'package org.lyaaz.fuckshare.exifhelper\nimport java.io.*\ninterface ExifHelper { fun removeMetadata(inputStream: InputStream, outputStream: OutputStream); fun postProcess(file: File) {} }',
        'FileUtils.kt': 'package org.lyaaz.fuckshare.utils\nimport java.io.*\nobject FileUtils { private const val OPTIMAL_BUFFER_SIZE = 65536\n' + method(fileutils, '    fun copy(') + '\n}',
        'Regression.kt': (ROOT / 'verification/ParserRegression.kt').read_text(),
    })

    process = method(fileutils, '    private fun processImage(')
    run('file-fallback', [], {
        'FileProbe.kt': (ROOT / 'verification/FileProbe.kt').read_text()
            .replace('// PRODUCTION_PROCESS_IMAGE', process)
            .replace('// PRODUCTION_FILE_NAME', method(fileutils, '    private fun getRealFileName(')),
        'Logging.kt': logging,
        'Regression.kt': (ROOT / 'verification/FileFallbackRegression.kt').read_text(),
    })
