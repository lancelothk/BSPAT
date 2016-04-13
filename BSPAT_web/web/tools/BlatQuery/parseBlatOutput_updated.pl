#!/usr/local/bin/perl -w

use strict;

my $argc = @ARGV;	#	arg count

sub usage() {
    print "usage: parseBlat.pl <output type> <html output> [other html outputs...]\n";
    print "\toutput type is psl or hyperlink\n";
    print "\t<html output> - file with html returned from blat request\n";
    print "\t[other html outputs...] - more html file results \n";
    print "\toutput is to stdout\n";
}
if ($argc < 1) {
    usage;
    exit 255;
}

sub processPsl {
   while (my $FName = shift) {
       my $lineCount = 0;
       open (FH, "$FName") or die "Can not open $FName";
       my $inPsl = 0;
       my $header = 0;
       my $hCount = 0;
       while (my $line = <FH>) {
          ++$lineCount;
	  chomp($line);
	  if ($line =~ m#^.*<TT><PRE>#) {
	      $line =~ s#^.*<TT><PRE>##;
	      $inPsl = 1;	#	have encountered the PSL output
	  }
	  if ($line =~ m#^</PRE></TT>$#) {
	      $inPsl = 0;	#	at end of PSL output
	  }
	  if ($inPsl) {
              # if psl header exists then print
              if ($line =~ m/(psLayout\s?version\s?[0-9]+.+)/) {
                  my $ps = $1;
                  if ($hCount < 5) {
                      print "$ps\n"; # only print if first time
                  }
                  $header = 1;
              }
              elsif (($header) && ($hCount < 5)) {
                  print "$line\n";
                  $hCount++;
                  if ($hCount == 5) {
                      $header =0; # reset after printing header
                  }
              } 
              elsif ($header) {
              }
              else {
	          my @a = split('\s',$line);  #	process a PSL output line
	          my $fields = @a;
	          if ($fields != 21) {
	              print STDERR "WARNING did not find 21 fields at line: $lineCount in file: $FName\n";
		      print STDERR "WARN: $line\n";
		      next;
	          }
                  # print out each field of the output
	          print "$line\n";
	      }
          } 
      } # end of loop to read file
   close (FH);
   } # end of while loop to process files
} # end of processPsl function


sub processHyperLink {
   while (my $FName = shift) {
       my $lineCount = 0;
       open (FH, "$FName") or die "Can not open $FName";
       my $inLinks = 0;
       while (my $line = <FH>) {
          ++$lineCount;
	  chomp($line);
	  if ($line =~ m#<H2>BLAT\sSearch\sResults#) {
              $line =~ s#^\s<H2>##;
              $line =~ s#<\/H2><TT><PRE>\s+ACTIONS\s+#\n#;
              print "$line\n";
	      $inLinks = 1;	# have encountered the hyperlinked output
	  }
          elsif ($line =~ m#^[\-]+#) {
              print "$line\n";
          }
	  elsif ($line =~ m#^</PRE></TT>$#) {
	      $inLinks = 0;	#	at end of hyperlinked output
	  }
	  elsif ($inLinks) {
              $line =~ s#^<A\sHREF.+<\/A>\s##;
              print "$line\n"; 
          } # end of if reading hyperlinks
       } # end of loop to read file
   } # end of while to go through input files
} # end of subroutine

my $outputType = shift;
if ($outputType eq "psl") {
   processPsl(@ARGV);
}
elsif ($outputType eq "hyperlink") {
   processHyperLink(@ARGV);
}
else {
   print "ERROR: have not specified an acceptable output file type, it should be
psl or hyperlink.";
   exit 255;
}
