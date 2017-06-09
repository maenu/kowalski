# Cite KOWALSKI

## Paper and Presentation

KOWALSKI was presented at SATToSE 2017 in Madrid.

	@misc{Leue17x,
		author = {Manuel Leuenberger},
		title = {Exploiting Dependency Management Systems to Learn about API Usage: A Lucene Case Study},
		howpublished = {Paper and presentation at SATToSE 2017},
		month = jun,
		year = {2017},
		url = {http://sattose.wdfiles.com/local--files/2017:schedule/SATToSE_2017_paper_20.pdf}
	}

## Master Thesis

The tool was originally developed under the name COLUMBO in a master thesis.
Later it was renamed to KOWALSKI to avoid name conflicts.
The thesis contains a detailed description of its architecture.

	@mastersthesis{Leue17a,
		Title = {Nullable Method Detection --- Inferring Method Nullability From {API} Usage},
		Author = {Manuel Leuenberger},
		Abstract = {Null dereferences are the cause of many bugs in Java projects.
			Avoiding them is hard, as they are not detected by the compiler.
			Many of those bugs are caused by dereferencing values returned from
			methods. This finding implies that developers do not anticipate
			which methods possibly return null and which do not. In this study
			we detect the nullable methods within Apache APIs by analyzing
			their usage in API clients. We compute the nullability of each
			invoked method, i.e., the ratio between null-checked and all
			dereferenced method return values. To collect many API clients of
			Apache API, we perform a targeted API client collection. Our tool,
			COLUMBO, exploits the widespread use of the Maven dependency
			management to find clients of Apache APIs. COLUMBO is fast and
			scalable. We collect and analyze 45638 Apache API clients and
			measure 31.4% of conditional expressions to be null checks. We find
			65.0% of dereferenced return values of Apache API methods are never
			checked for null, 33.5% are sometimes checked and 1.5% are always
			checked. A manual inspection of the methods rarely checked in
			client usage shows that about a third of them can never return
			null, hence checking the return value for null is superfluous and
			hinders code readability. In the Apache API clients we also analyze
			their usage of the JRE and we find a similar nullability
			distribution as in Apache usage. We consider method nullability an
			important part of a method contract, but we find it to be
			incompletely documented in the JRE API documentation. Most method
			documentations do not make a statement about their nullability. To
			bridge this gap, we integrate the nullability data in an IDE plugin
			that shows developers the measured nullability for each method,
			giving them an estimation of the potential null return.},
		Keywords = {scg-msc snf-asa2 scg17 jb17},
		School = {University of Bern},
		Type = {Masters thesis},
		Url = {http://scg.unibe.ch/archive/masters/Leue17a.pdf},
		Month = feb,
		Year = {2017}
	}
