/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { FACTS, groupCard, type TaxonCard, type TaxonData, type TreeDemoTexts } from '../tree.js';

/** The card of a group or a species, as the Wicket page shows it. */
function Card({ card, kind, texts }: { card: TaxonCard; kind: 'group' | 'species';
	texts: TreeDemoTexts }) {
	return (
		<article className={'tree-demo-card tree-demo-card--' + kind}>
			<p className="tree-demo-card__rank">{card.rank}</p>
			<h3 className="tree-demo-card__name">{card.name}</h3>
			{card.scientific && <p className="tree-demo-card__scientific">{card.scientific}</p>}
			<p className="tree-demo-card__description">{card.description}</p>
			<dl className="tree-demo-card__facts">
				{FACTS.map((fact) => [
					<dt key={fact + '-label'}>{texts[fact]}</dt>,
					<dd key={fact}>{card[fact]}</dd>
				])}
			</dl>
		</article>
	);
}

/**
 * The group or the species selected in a tree of the demos, as the Wicket page shows it: its
 * lineage, the card of its group and, for a species, its own card below; or an invitation to
 * select one.
 */
export function TaxonDetails({ taxon, texts }: { taxon: TaxonData | null;
	texts: TreeDemoTexts }) {
	const group = taxon && groupCard(taxon);
	return (
		<section className="tree-demo-taxon" aria-live="polite">
			<h2 className="tree-demo-taxon__title">{texts.selected}</h2>
			{taxon ? (
				<>
					<ol className="tree-demo-taxon__lineage" aria-label={texts.lineage}>
						{taxon.lineage.map((name, index) => (
							<li key={index} aria-current={index === taxon.lineage.length - 1 ?
								'true' : undefined}>{name}</li>
						))}
					</ol>
					{group && <Card card={group} kind="group" texts={texts} />}
					{!taxon.group && <Card card={taxon} kind="species" texts={texts} />}
				</>
			) : <p className="tree-demo-taxon__none">{texts.noneSelected}</p>}
		</section>
	);
}
