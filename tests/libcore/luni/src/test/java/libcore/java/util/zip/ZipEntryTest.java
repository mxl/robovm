/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package libcore.java.util.zip;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipEntryTest extends junit.framework.TestCase {
  private static File createTemporaryZipFile() throws IOException {
    File result = File.createTempFile("ZipFileTest", "zip");
    result.deleteOnExit();
    return result;
  }

  private static ZipOutputStream createZipOutputStream(File f) throws IOException {
    return new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
  }

  private static String makeString(int count, String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < count; ++i) {
      sb.append(s);
    }
    return sb.toString();
  }

  // http://code.google.com/p/android/issues/detail?id=4690
  public void test_utf8FileNames() throws Exception {
    // Create a zip file containing non-ASCII filenames.
    File f = File.createTempFile("your", "mum");
    List<String> filenames = Arrays.asList("us-ascii",
                                           "\u043c\u0430\u0440\u0442\u0430", // russian
                                           "\u1f00\u03c0\u1f78", // greek
                                           "\u30b3\u30f3\u30cb\u30c1\u30cf"); // japanese
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
    for (String filename : filenames) {
      out.putNextEntry(new ZipEntry(filename));
      out.closeEntry(); // Empty files are fine.
    }
    out.close();
    // Read it back, and check we find all those names.
    // This failed when we were mangling the encoding.
    ZipFile zipFile = new ZipFile(f);
    for (String filename : filenames) {
      assertNotNull(filename, zipFile.getEntry(filename));
    }
    // Check that ZipInputStream works too.
    ZipInputStream in = new ZipInputStream(new FileInputStream(f));
    ZipEntry entry;
    int entryCount = 0;
    while ((entry = in.getNextEntry()) != null) {
      assertTrue(entry.getName(), filenames.contains(entry.getName()));
      ++entryCount;
    }
    assertEquals(filenames.size(), entryCount);
    in.close();
  }

  // http://b/2099615
  public void testClone() {
    byte[] extra = { 5, 7, 9 };
    JarEntry jarEntry = new JarEntry("foo");
    jarEntry.setExtra(extra);
    assertSame("Expected no defensive copy of extra", extra, jarEntry.getExtra());

    ZipEntry clone = (ZipEntry) jarEntry.clone();
    assertEquals(JarEntry.class, clone.getClass());
    assertNotSame(extra, clone.getExtra());
  }

  public void testTooLongName() throws Exception {
    String tooLongName = makeString(65536, "z");
    try {
      new ZipEntry(tooLongName);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMaxLengthName() throws Exception {
    String maxLengthName = makeString(65535, "z");

    File f = createTemporaryZipFile();
    ZipOutputStream out = createZipOutputStream(f);
    out.putNextEntry(new ZipEntry(maxLengthName));
    out.closeEntry();
    out.close();

    // Read it back, and check that we see the entry.
    ZipFile zipFile = new ZipFile(f);
    assertNotNull(zipFile.getEntry(maxLengthName));
    zipFile.close();
  }

  public void testTooLongExtra() throws Exception {
    byte[] tooLongExtra = new byte[65536];
    ZipEntry ze = new ZipEntry("x");
    try {
      ze.setExtra(tooLongExtra);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMaxLengthExtra() throws Exception {
    byte[] maxLengthExtra = new byte[65535];

    File f = createTemporaryZipFile();
    ZipOutputStream out = createZipOutputStream(f);
    ZipEntry ze = new ZipEntry("x");
    ze.setExtra(maxLengthExtra);
    out.putNextEntry(ze);
    out.closeEntry();
    out.close();

    // Read it back, and check that we see the entry.
    ZipFile zipFile = new ZipFile(f);
    assertEquals(maxLengthExtra.length, zipFile.getEntry("x").getExtra().length);
    zipFile.close();
  }

  public void testTooLongComment() throws Exception {
    String tooLongComment = makeString(65536, "z");
    ZipEntry ze = new ZipEntry("x");
    try {
      ze.setComment(tooLongComment);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMaxLengthComment() throws Exception {
    String maxLengthComment = makeString(65535, "z");

    File f = createTemporaryZipFile();
    ZipOutputStream out = createZipOutputStream(f);
    ZipEntry ze = new ZipEntry("x");
    ze.setComment(maxLengthComment);
    out.putNextEntry(ze);
    out.closeEntry();
    out.close();

    // Read it back, and check that we see the entry.
    ZipFile zipFile = new ZipFile(f);
    assertEquals(maxLengthComment, zipFile.getEntry("x").getComment());
    zipFile.close();
  }

  public void testCommentAndExtraInSameOrder() throws Exception {
    String comment = makeString(17, "z");
    byte[] extra = makeString(11, "a").getBytes();

    File f = createTemporaryZipFile();
    ZipOutputStream out = createZipOutputStream(f);
    ZipEntry ze = new ZipEntry("x");
    ze.setExtra(extra);
    ze.setComment(comment);
    out.putNextEntry(ze);
    out.closeEntry();
    out.close();

    // Read it back and make sure comments and extra are in the right order
    ZipFile zipFile = new ZipFile(f);
    try {
      assertEquals(comment, zipFile.getEntry("x").getComment());
      assertTrue(Arrays.equals(extra, zipFile.getEntry("x").getExtra()));
    } finally {
      zipFile.close();
    }
  }
}
