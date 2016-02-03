package com.databasepreservation.siarddk;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;

import com.databasepreservation.modules.siard.out.content.lobs.SingleCharacterWriteLinesStrategy;
import com.databasepreservation.modules.siard.out.content.lobs.TiffPage;

/**
 * @author Andreas Kring <andreas@magenta.dk>
 *
 */
public class TiffPageDriver {

  /**
   * @param args
   */
  public static void main(String[] args) {
    TiffPage tiffPage = new TiffPage(300, 300, 20, 20, 15, new SingleCharacterWriteLinesStrategy());

    String clob = "Dette er en meget meget lang streng, som skal skrives til det dynamiske og farverige billede";
    // clob =
    // "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse in euismod sapien. Donec urna quam, interdum sit amet est quis, scelerisque tempus magna. Curabitur vehicula, lectus eu elementum suscipit, lectus lorem feugiat eros, in feugiat quam metus quis mi. Nullam dui velit, ornare eget egestas a, ornare varius lorem. Sed id sem turpis. Ut laoreet arcu et sapien placerat, vitae rutrum nisi viverra. Aliquam vitae lorem diam. Vivamus ac lectus rhoncus, feugiat augue sit amet, ullamcorper enim. Nam at purus dolor. Duis bibendum tempor nunc. Sed sodales justo nec enim accumsan, in aliquet nisl dignissim. Curabitur tempor sagittis enim, sed tincidunt dolor blandit a. Morbi pharetra, lorem in ultricies pharetra, velit justo viverra ex, non cursus arcu nibh vel nisl. Etiam gravida elementum sollicitudin. Vestibulum mauris augue, scelerisque eu ipsum eget, aliquam molestie ligula. Duis euismod risus eu libero faucibus, vitae feugiat lorem laoreet. Morbi vestibulum dignissim sapien, vitae ultricies arcu interdum sed. Ut aliquam ut nisi tincidunt volutpat. Nulla eu orci a nulla fermentum sodales. Cras at sodales augue. Cras quis lobortis eros. In vestibulum augue et nunc ultricies, et pulvinar erat eleifend. Sed ut cursus urna. Maecenas nec cursus sapien.Cras lorem justo, sodales sit amet est id, feugiat varius nibh. Proin vulputate posuere leo id auctor. Vivamus tincidunt nisi vel erat consectetur, et dictum nibh aliquet. Nulla at lorem consequat massa dapibus tristique nec nec mauris. Cras pulvinar ligula vel risus porttitor, sed mattis ligula mollis. Mauris blandit lorem a rutrum sagittis. Donec finibus lacus ipsum, ut fermentum eros laoreet ut. Integer ornare lorem quis tellus feugiat rhoncus. Sed feugiat magna sed orci tempus, maximus tristique risus vulputate. Sed eu mi sapien. Nunc commodo sem sed ex congue sodales. Proin diam urna, iaculis eget pretium id, sollicitudin commodo turpis. Aliquam mattis fringilla facilisis. Fusce commodo, massa in tempor venenatis, turpis mi malesuada elit, at tempor urna nisi in ipsum. Fusce arcu mauris, pulvinar at odio ut, volutpat pellentesque nibh. Pellentesque efficitur faucibus odio. Proin egestas, ex id accumsan semper, erat dui consectetur turpis, sit amet elementum metus sem laoreet erat. Nulla nec nisl non massa pretium luctus. Sed vehicula enim vel eros fermentum ultrices. Sed auctor sed augue nec ultrices. Quisque pretium porta lectus id mollis. In ultricies, velit sed semper dignissim, lacus justo tincidunt felis, eu hendrerit tortor dolor sit amet tortor. Aenean posuere, justo quis blandit malesuada, tellus sapien tempus dui, in gravida metus magna eu odio. Sed pharetra nunc a massa interdum ornare. Etiam pharetra sit amet elit a finibus. Fusce nulla neque, porttitor in commodo vel, fringilla at dui. Mauris non metus elit. Donec porttitor viverra sagittis. Nulla facilisi. Praesent sit amet interdum nibh, ut convallis leo. Nam et elit lectus. Pellentesque id risus molestie, malesuada magna sit amet, rhoncus dolor. Curabitur gravida dui eu lacus pulvinar fringilla. Sed eu vehicula lacus, id dignissim turpis. ";

    char[] characters = new char[clob.length()];
    clob.getChars(0, clob.length(), characters, 0);

    OutputStream stream;
    try {
      stream = new FileOutputStream("/tmp/img.tiff");
      char[] remainingChars = tiffPage.writeCLOBToOutputStream(characters, stream);
      System.out.println(remainingChars.length);
      System.out.println(Arrays.toString(remainingChars));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    // System.out.println("Rest = " +
    // Arrays.toString(tiffPage.writeLines(characters)));
    // tiffPage.saveTiff();

  }

}
