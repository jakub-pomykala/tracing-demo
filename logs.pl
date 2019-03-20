
while (<>) 
{
    $makerpod = $1 if ($_ =~ /(maker.+?) /); $pods{$makerpod} = "maker-bot";
    $ics = $1 if ($_ =~ /(instrument.+?) /); $pods{$ics} = "instrument-craft-shop";
    $dbpod = $1 if ($_ =~ /(dbwrapper.+?) /); $pods{$dbpod} = "dbwrapper";

    $p1 = $1 if ($_ =~ /(pipeline-n1.+?) /); $pods{$p1} = "pipeline-node";
    $p2 = $1 if ($_ =~ /(pipeline-n2.+?) /); $pods{$p2} = "pipeline-node";
#    $p3 = $1 if ($_ =~ /(pipeline-n3.+?) /); $pods{$p3} = "pipeline-node";
#    $p4 = $1 if ($_ =~ /(pipeline-n4.+?) /); $pods{$p4} = "pipeline-node";
#    $p5 = $1 if ($_ =~ /(pipeline-n5.+?) /); $pods{$p5} = "pipeline-node";
}
$cmd = " -hold -fa 'Monospace' -fs 10 -e kubectl logs -f";

for (keys %pods) {
    print "pod: $_ container name: $pods{$_} \n";
    system("xterm -T $_ $cmd $_  $pods{$_} &");
}


    
