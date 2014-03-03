#!/usr/bin/perl
use strict;

my $barcodes = $ARGV[0];
my $pair1 = $ARGV[1];
my $outputPath = $ARGV[2];
my $output = \*STDOUT;

open PAIR1, "$pair1";

open (BARCODE, "<$barcodes") || die "Cannot Open barcodes File";

my %fwdBarcodes;
my %fh1Barcodes;
while (<BARCODE>) {
	#chomp;
	my @values = split('\t');
	my $keyName = $values[0];
	#trim both \n or \r\n
	$values[1] =~ s/\r?\n$//;
	
	$fwdBarcodes{$keyName} = $values[1];
	my $fh1;
	open($fh1, ">". $outputPath . "/$keyName.fq") || die "Cannot write to file $keyName.txt";
	$fh1Barcodes{$keyName} = $fh1;
}
close(BARCODE);
my $totalNbReads = 0;
my $foundNbReads = 0;
open (PAIR1, "$pair1") || die "Cannot Open pair 1 File";
while(my $seq1Header = <PAIR1>) {
	my $seq1 = <PAIR1>;
	my $qua1Header = <PAIR1>;
	my $qua1 = <PAIR1>;

	chomp($seq1Header);
	chomp($seq1);
	chomp($qua1Header);
	chomp($qua1);

	my $head1 = substr($seq1Header, 0, length ($seq1Header)-2);
	my $key;
	$totalNbReads++;
	foreach $key ( keys %fwdBarcodes ) {
		my $key1fwd = substr($seq1, 0, length($fwdBarcodes{$key}));
		if($fwdBarcodes{$key} eq $key1fwd){
			$foundNbReads++;
			my $fh1 = $fh1Barcodes{$key};
			print $fh1 "$seq1Header\n";
			#just output seq without barcode
			#print $fh1 substr($seq1,length($key1fwd))."\n";
			#output seq with barcode
			print $fh1 $seq1 ."\n";
			print $fh1 "$qua1Header\n";
			#output seq with barcode
			#print $fh1 substr($qua1,length($key1fwd))."\n";
			print $fh1 $qua1 ."\n";
			last;
		}
	}
}

foreach my $key ( keys %fwdBarcodes ) {
	close("$key.txt");
}
close(PAIR1);
print $output "Total nb reads: $totalNbReads    Found reads: $foundNbReads   Percent: ".($foundNbReads*100/$totalNbReads)."\n";
