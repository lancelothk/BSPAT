#!/usr/local/bin/perl -w

use strict;

use HTTP::Request::Common;
use LWP::UserAgent;
use HTTP::Cookies;

#	doc for above classes at:
#	http://cpan.uwinnipeg.ca/htdocs/libwww-perl/HTTP/Request/Common.html
#	http://cpan.uwinnipeg.ca/htdocs/libwww-perl/HTTP/Request.html
#	http://cpan.uwinnipeg.ca/htdocs/libwww-perl/LWP/UserAgent.html

my $argc = @ARGV;	#	arg count

my $firstTime = 1;
my $batchCount = 25;	#	number of sequences to submit in one request
my $batchNum = 1;       # batch number
my $sleepTime = 0;	#	seconds to sleep after request returned
# searchType is BLAT's guess, DNA, protein, translated RNA, translated DNA
my ($org, $db, $searchType, $sortOrder, $FName, $outputType, $outFile);
($org, $db, $searchType, $sortOrder, $FName, $outputType, $outFile) = @ARGV;

sub Usage() {
    print "usage: BlatBot.pl <organism> <db> <searchType> <sortOrder>";
    print " <input FASTA> <outputType> <output file>\n";
    print "\tSpecify organism using the common name with first letter";
    print "capitalized.\n";
    print "\te.g. Human, Mouse, Rat etc.\n";
    print "\tDb is database or assembly name e.g hg17, mm5, rn3 etc.\n";
    print "\tsearchType can be BLATGuess, DNA, RNA, transDNA or transRNA\n";
    print "\tsortOrder can be query,score; query,start; chrom,score;\n";
    print "\tchrom,start; score.\n";
    print "\toutputType can be pslNoHeader, psl or hyperlink.\n";
    print "\tblats will be run in groups of $batchCount sequences, all\n";
    print "\toutput going to the specified output file.\n";
}

if ($argc != 7) {
    Usage;
    exit 255;
}

if ($searchType eq "BLATGuess") {
    $searchType = "Blat's Guess";
} elsif ($searchType eq "transDNA") {
    $searchType = "translated DNA";
} elsif ($searchType eq "transRNA") {
    $searchType = "translated RNA";
} elsif (($searchType eq "DNA") || ($searchType eq "RNA")) {
} else {
    print "ERROR: have not specified an acceptable search type - it should be BLATGuess, transDNA, transRNA, DNA or RNA.";
    Usage;
    exit 255;
}
if ($outputType eq "pslNoHeader") {
     $outputType = "psl no header";
} elsif (($outputType eq "psl") || ($outputType eq "hyperlink")) {
} else {
    print "ERROR: have not specified an acceptable output type - it should be pslNoHeader, psl or hyperlink.";
    Usage;
    exit 255;
}

my $response;
my $url = 'http://genome.ucsc.edu/cgi-bin/hgBlat/hgBlat';
my $cookieFile = '/tmp/blatCookies';

my $ua = LWP::UserAgent->new;
#	set agent name
$ua->agent("botBlat");

$ua->timeout(600);	#	10 minute time-out
# initialize cookies, this creates a file with the hguid so it can be reused
# for each batch and therefore prevent userDb from filling up
$ua->cookie_jar(HTTP::Cookies->new(file     => $cookieFile,
                                   autosave => 1));
print "\nSet up cookie jar in $cookieFile\n\n";

open (FH, "<$FName") or die "Can not open $FName";

my $seqCount = 0;
my $fasta = "";
my $hgsid = 0;

sub oneBatch() {
open (OUT,">>$outFile") or die "Can not open output file $outFile";
    print STDERR "Running sequences ...\n";
    # if an hgsid was obtained from the output of the first batch
    # then use this.
    if ($hgsid) {
         $response = $ua->request(POST $url, 
            [org=>$org, db=>$db, type=>$searchType, sort=>$sortOrder, 
            output=>$outputType, userSeq=>$fasta, hgsid=>$hgsid]);
    }
    else {
         $response = $ua->request(POST $url, 
              [org=>$org, db=>$db, type=>$searchType, sort=>$sortOrder, 
              output=>$outputType, userSeq=>$fasta]);
    }
    if ($response->is_success) {
	print OUT $response->content;
    } else {
	print STDERR "did not receive an is_success, dying, content is:";
	print STDERR $response->content;
	print OUT $response->content;
	die $response->status_line;
    }
    close OUT;
}

sub getHgsid {
    my ($file, $line, $hgsid, $found);
    $file = shift;
    $hgsid = 0;
    $found = 0;
    # open output file and parse out hgsid and reuse this for each batch
    # to stop the sessionDb filling up
    print STDERR "Opening output file $file to retrieve hgsid\n";
    open(FILE, "<$file") || die "Can not open $file: $!\n";
    while (<FILE>) {
       $line = $_;
       if ($line =~ m/hgsid=([0-9]+)/ && (! $found)) {
           $hgsid = $1;
           $found = 1;
       }
       if ($found) {
            close FILE;
            return $hgsid;
       }
    }
    return $hgsid;
}

my $lineCount = 0;

my $line = <FH>;
++$lineCount;
while ($line) {
    if ($line =~ m/^>/) {
	++$seqCount;
	$fasta = $fasta . $line;
	$line = <FH>;
	++$lineCount;
	while ($line) {
	    if ($line =~ m/^>.*/) {
		last;
	    };
	    $fasta = $fasta . $line;
	    $line = <FH>;
	    ++$lineCount;
	}
    } else {
	print STDERR "ERROR: got lost in the fasta record ? line: $lineCount\n";
	print STDERR "$line";
	exit 255;
    }
    if ($seqCount >= $batchCount) {
	if (! $firstTime) {
	    sleep ($sleepTime);
            if ($batchNum == 1) {
                $hgsid = getHgsid($outFile);
            }
            $batchNum++;
	}
	$firstTime = 0;
	oneBatch;
	$fasta = "";
	$seqCount = 0;
    }
}

# possible last one
if ($seqCount > 0) {
    sleep ($sleepTime);
    oneBatch;
}

close (OUT);
close (FH);
